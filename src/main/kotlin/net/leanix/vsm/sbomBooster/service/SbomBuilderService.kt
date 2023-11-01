package net.leanix.vsm.sbomBooster.service

import com.github.packageurl.PackageURL
import com.github.packageurl.PackageURLBuilder
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.domain.VsmSbomBoosterUtils
import org.cyclonedx.BomGeneratorFactory
import org.cyclonedx.CycloneDxSchema
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Component
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice
import org.cyclonedx.model.Metadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.util.Date

@Service
class SbomBuilderService(
    private val gitHubApiService: GitHubApiService,
    private val clearlyDefinedService: ClearlyDefinedService
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SbomBuilderService::class.java)
        const val SBOM_FILE_NAME: String = "bom.cyclonedx.json"
    }

    @Suppress("NestedBlockDepth")
    fun fromGithubDependencyGraph(
        propertiesConfiguration: PropertiesConfiguration,
        token: String,
        repository: Repository,
        outputFolder: String
    ): Boolean {
        try {

            val dependencies = gitHubApiService.getAllDependencies(
                token,
                propertiesConfiguration.githubOrganization,
                repository.name
            )

            if (dependencies.isEmpty()) {
                logger.warn(
                    "No dependencies found\n\nIf you own this repository, check if Dependency Graph is enabled:\n" +
                        "https://github.com/{args.organization}/{args.repository}/settings/security_analysis\n\n"
                )
                return false
            }

            val components = mutableListOf<Component>()
            for ((_, packageManager) in dependencies) {
                for ((_, packageObj) in packageManager.packages) {
                    for ((version, _) in packageObj.versions) {
                        val component = buildComponent(packageManager.name, packageObj.name, version)

                        components.add(component)
                    }
                }
            }

            val component = Component()
            component.name = repository.name
            component.group = propertiesConfiguration.githubOrganization

            val metadata = Metadata()
            metadata.component = component
            metadata.timestamp = Date()

            val bom = Bom()
            bom.components = components
            bom.metadata = metadata

            val generatedSbom = BomGeneratorFactory.createJson(CycloneDxSchema.Version.VERSION_14, bom).toJsonString()

            VsmSbomBoosterUtils.writeStringToFile(
                Paths.get("tempDir", outputFolder).toString(),
                SBOM_FILE_NAME,
                generatedSbom
            )

            return true
        } catch (e: Exception) {
            logger.error("Message: {}\nStack: {}", e.message, e.stackTrace)
            return false
        }
    }

    private fun buildComponent(packageManagerName: String, packageName: String, version: String): Component {
        val purl = Purl(packageManagerName, packageName, version)

        val (declaredLicensesExpression, discoveredLicensesExpression, _) =
            clearlyDefinedService.getLicense(purl)

        val licensesAdded: MutableMap<String, Boolean> = mutableMapOf()
        val licenseChoice = LicenseChoice()
        if (declaredLicensesExpression.isNotEmpty()) {
            parseLicenses(declaredLicensesExpression).forEach {
                if (!licensesAdded.containsKey(it.name)) {
                    licenseChoice.addLicense(it)
                    licensesAdded[it.name] = true
                }
            }
        }
        if (discoveredLicensesExpression.isNotEmpty()) {
            parseLicenses(discoveredLicensesExpression).forEach {
                if (!licensesAdded.containsKey(it.name)) {
                    licenseChoice.addLicense(it)
                    licensesAdded[it.name] = true
                }
            }
        }

        val component = Component()

        component.type = Component.Type.LIBRARY
        component.name = purl.packageName
        component.version = purl.version
        component.purl = purl.packageURL.toString()
        component.group = purl.namespace
        component.licenseChoice = licenseChoice

        return component
    }

    private fun parseLicenses(licenseExpression: String): List<License> {
        val regex = Regex("\\b(?!AND|OR)\\w+(-\\w+)*\\b")
        return regex.findAll(licenseExpression).map {
            val license = License()
            license.name = it.value
            license
        }.toList()
    }
}

class Purl(
    packageManagerName: String,
    var packageName: String,
    var version: String
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Purl::class.java)
    }

    var provider: String? = null
    var namespace: String? = null
    var cdType: String? = null
    var cdProvider: String? = null
    var packageURL: PackageURL? = null

    init {
        when (packageManagerName) {
            "actions" -> {
                cdType = "git"
                cdProvider = "github"
                provider = "githubactions"
                val packageParts = packageName.split("/", limit = 2)
                if (packageParts.size >= 2) {
                    this.namespace = packageParts[0]
                    this.packageName = packageParts[1]
                }
            }
            "go" -> {
                cdType = "go"
                cdProvider = "golang"
                provider = "golang"
                val packageParts = packageName.split("/", limit = 3)
                if (packageParts.size == 2) {
                    this.namespace = packageParts[0]
                    this.packageName = packageParts[1]
                } else if (packageParts.size == 3) {
                    this.namespace = packageParts[0] + "/" + packageParts[1]
                    this.packageName = packageParts[2]
                }
            }
            "rubygems" -> {
                cdType = "gem"
                cdProvider = "rubygems"
                provider = "gem"
            }
            "maven" -> {
                cdType = "maven"
                cdProvider = "mavenCentral"
                provider = "maven"
                val packageParts = packageName.split(":", limit = 2)
                if (packageParts.size >= 2) {
                    this.namespace = packageParts[0]
                    this.packageName = packageParts[1]
                }
            }
            "npm" -> {
                cdType = "npm"
                cdProvider = "npmjs"
                provider = "npm"
                if (packageName.startsWith("@")) {
                    val packageParts = packageName.split("/", limit = 2)

                    if (packageParts.size >= 2) {
                        this.namespace = packageParts[0]
                        this.packageName = packageParts[1]
                    }
                }
            }
            "pip" -> {
                cdType = "pypi"
                cdProvider = "pypi"
                provider = "pypi"
            }
            else -> {
                logger.warn("WARNING: Unknown Package manager '$packageManagerName'")
            }
        }
        packageURL = PackageURLBuilder.aPackageURL()
            .withType(provider)
            .withNamespace(namespace)
            .withName(packageName)
            .withVersion(version)
            .build()
        logger.debug("built purl : {}", packageURL)
    }
}
