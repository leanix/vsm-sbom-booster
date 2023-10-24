package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.domain.VsmDiscoveryItem
import net.leanix.vsm.sbomBooster.domain.VsmSbomBoosterUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class ProcessService(
    private val ortService: OrtService,
    private val cycloneDxCliService: CycloneDxCliService,
    private val vsmDiscoveryService: VsmDiscoveryService,
    private val mtMService: MtMService,
    private val sbomBuilder: SbomBuilder,
    private val propertiesConfiguration: PropertiesConfiguration
) {
    companion object {
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val logger: Logger = LoggerFactory.getLogger(ProcessService::class.java)
    }

    fun initOrt() {
        logger.info("Pulling the latest version of ORT...")
        ortService.pullOrt()
        logger.info("Pulled the latest version of ORT!")

        if (propertiesConfiguration.gitProvider.uppercase() == "GITHUB" &&
            propertiesConfiguration.githubDependencyGraph
        ) {
            logger.info("Pulling the latest version of CycloneDX CLI...")
            cycloneDxCliService.pullCdxCli()
            logger.info("Pulled the latest version of CycloneDX CLI!")
        }
    }

    @Async
    fun processRepository(
        username: String,
        token: String,
        repository: Repository
    ) {
        val startInstant = Instant.now()
        val downloadedFolder: String = randomDownloadFolder(repository.cloneUrl)
        var gitProviderSbomFolder: String? = null
        var ortFolder: String? = null
        var mergedSbomsFolder: String? = null
        if (username.isNotBlank()) {
            try {
                gitProviderSbomFolder = generateGitProviderSbom(token, repository)

                ortFolder = generateOrtSbom(repository, username, token, downloadedFolder)

                if (gitProviderSbomFolder != null) {
                    mergedSbomsFolder = mergeSboms(repository, ortFolder, gitProviderSbomFolder)
                }

                if (mergedSbomsFolder != null)
                    uploadSbom(propertiesConfiguration, repository, mergedSbomsFolder)
                else
                    uploadSbom(propertiesConfiguration, repository, ortFolder)
            } catch (e: Exception) {
                logger.error(e.message)
            } finally {
                VsmSbomBoosterUtils.deleteFolder(downloadedFolder)

                if (!propertiesConfiguration.devMode) {
                    VsmSbomBoosterUtils.deleteFolder(ortFolder)
                    VsmSbomBoosterUtils.deleteFolder(gitProviderSbomFolder)
                    VsmSbomBoosterUtils.deleteFolder(mergedSbomsFolder)
                }
            }
        }

        val duration = (
            Instant.now().toEpochMilli() -
                startInstant.toEpochMilli()
            ).toDuration(DurationUnit.MILLISECONDS)

        logger.info(
            "Processed repository with url: ${repository.cloneUrl} in $duration"
        )
    }

    private fun generateGitProviderSbom(
        token: String,
        repository: Repository
    ): String? {
        val gitProvidedSbomFolder = "${repository.cloneUrl.substringAfterLast("/")}_GIT_Provided_SBOM"

        if (propertiesConfiguration.gitProvider.uppercase() == "GITHUB" &&
            propertiesConfiguration.githubDependencyGraph
        ) {
            logger.info("Beginning to generate CycloneDX file from GitHub Graph")
            val created =
                sbomBuilder.fromGithubDependencyGraph(propertiesConfiguration, token, repository, gitProvidedSbomFolder)
            return if (created)
                gitProvidedSbomFolder
            else
                null
        }
        return null
    }

    private fun generateOrtSbom(
        repository: Repository,
        username: String,
        token: String,
        downloadedFolder: String
    ): String {
        logger.info("Beginning to download repository with url: ${repository.cloneUrl}")
        ortService.downloadProject(
            repository.cloneUrl,
            username,
            token,
            downloadedFolder
        )
        logger.info(
            "Finished downloading repository with url: ${repository.cloneUrl} to temp folder: $downloadedFolder"
        )

        logger.info("Beginning to analyze repository with url: ${repository.cloneUrl}")
        val ortFolder = ortService.analyzeProject(repository.cloneUrl, downloadedFolder)
        logger.info(
            "Finished analyzing repository with url: ${repository.cloneUrl} in temp folder $ortFolder"
        )

        ortService.generateSbom(repository.cloneUrl)
        logger.info(
            "Finished generating SBOM file for repository with url: " +
                "${repository.cloneUrl} in temp folder $ortFolder."
        )

        return ortFolder
    }

    private fun mergeSboms(repository: Repository, ortFolder: String, gitProviderFolder: String): String {
        val mergedSbomFolder = "${repository.cloneUrl.substringAfterLast("/")}_merged_sbom"

        // Create the directory if it doesn't exist
        val directory = Paths.get("tempDir", mergedSbomFolder).toFile()
        if (!directory.exists()) {
            directory.mkdirs()
        }

        cycloneDxCliService.mergeSboms(
            "$ortFolder/bom.cyclonedx.json",
            "$gitProviderFolder/bom.cyclonedx.json",
            mergedSbomFolder
        )

        return mergedSbomFolder
    }

    private fun uploadSbom(
        propertiesConfiguration: PropertiesConfiguration,
        repository: Repository,
        bomFolder: String
    ) {
        val accessToken = mtMService.getAccessToken(
            propertiesConfiguration.leanIxHost,
            propertiesConfiguration.leanIxToken
        )

        val vsmRegion = extractVsmRegion(accessToken)

        vsmDiscoveryService.sendToVsm(
            accessToken,
            vsmRegion,
            VsmDiscoveryItem(
                repository.cloneUrl,
                bomFolder,
                repository.sourceType,
                repository.sourceInstance,
                repository.name,
                repository.repoId
            )
        )
    }

    private fun randomDownloadFolder(projectUrl: String): String {
        return "${projectUrl.substringAfterLast("/")}_${List(10) { charPool.random() }.joinToString("")}"
    }

    private fun extractVsmRegion(accessToken: String): String {
        val decoder = Base64.getDecoder()
        val payload = String(decoder.decode(accessToken.split(".")[1]))

        return payload.substringAfter("\"iss\":\"https://").substringBefore("-svc.leanix.net")
    }
}
