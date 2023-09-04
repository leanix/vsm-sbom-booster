package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class OrtService(
    private val propertiesConfiguration: PropertiesConfiguration
) {
    companion object {
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val logger: Logger = LoggerFactory.getLogger(OrtService::class.java)
    }

    fun pullOrt() {
        val pullOrtProcessBuilder = ProcessBuilder(
            "docker",
            "pull",
            "leanixacrpublic.azurecr.io/ort"
        )

        pullOrtProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val downloadProcess = pullOrtProcessBuilder.start()

        downloadProcess.waitFor(15, TimeUnit.MINUTES)
        downloadProcess.destroy()
    }

    fun downloadProject(projectUrl: String, username: String, gitToken: String): String {
        val downloadFolder = "${projectUrl.substringAfterLast("/")}_${List(10) { charPool.random() }.joinToString("")}"
        val args = mutableListOf(
            "sudo", "docker",
            "run", "--rm",
            "-e", "ORT_HTTP_USERNAME=$username",
            "-e", "ORT_HTTP_PASSWORD=$gitToken",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
            "-e", "ORT_CONFIG_DIR=/project/config"
        )

        addProxyEnvValues(args)
        val ortArgs = mutableListOf(
            "leanixacrpublic.azurecr.io/ort",
            loggingParameter(),
            "download",
            "--project-url", projectUrl,
            "-o", "/project/$downloadFolder"
        )
        addOrtArgs(args, ortArgs)

        val downloadProcessBuilder = ProcessBuilder(args)

        setupOutput(projectUrl, "download", downloadProcessBuilder)

        val downloadProcess = downloadProcessBuilder.start()

        downloadProcess.waitFor(15, TimeUnit.MINUTES)
        downloadProcess.destroy()

        return downloadFolder
    }

    fun analyzeProject(projectUrl: String, downloadFolder: String): String {

        val ortFolder = "${projectUrl.substringAfterLast("/")}_ORT_produced_files"

        val args = mutableListOf(
            "sudo", "docker", "run", "--rm",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/config:/config",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/$ortFolder:/ortProject",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/$downloadFolder:/downloadedProject",
            "-e", "ORT_CONFIG_DIR=/config",
        )

        addProxyEnvValues(args)

        val ortArgs = mutableListOf(
            "leanixacrpublic.azurecr.io/ort",
            loggingParameter(),
            "analyze",
            "-i", "/downloadedProject",
            "-o", "/ortProject"
        )

        addOrtArgs(args, ortArgs)

        logger.info("Running analyze command with the following arguments: $args")

        val analyzeProcessBuilder = ProcessBuilder(args)

        setupOutput(projectUrl, "analyze", analyzeProcessBuilder)

        val analyzeProcess = analyzeProcessBuilder.start()

        analyzeProcess.waitFor(propertiesConfiguration.analysisTimeout, TimeUnit.MINUTES)
        analyzeProcess.destroy()

        return ortFolder
    }

    fun generateSbom(projectUrl: String) {
        val args = mutableListOf(
            "sudo", "docker", "run", "--rm",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/config:/config",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/${projectUrl.substringAfterLast("/")}_ORT_produced_files:/ortProject",
            "-e", "ORT_CONFIG_DIR=/config",
        )

        addProxyEnvValues(args)
        val ortArgs = mutableListOf(
            "leanixacrpublic.azurecr.io/ort",
            loggingParameter(),
            "report",
            "-f", "CycloneDX",
            "-i", "/ortProject/analyzer-result.yml",
            "-o", "/ortProject",
            "-O", "CycloneDx=output.file.formats=json",
            "-O", "CycloneDx=schema.version=1.4"
        )

        addOrtArgs(args, ortArgs)

        val generateSbomProcessBuilder = ProcessBuilder(args)

        setupOutput(projectUrl, "generate_sbom", generateSbomProcessBuilder)

        val generateSbomProcess = generateSbomProcessBuilder.start()

        generateSbomProcess.waitFor(10, TimeUnit.MINUTES)
        generateSbomProcess.destroy()
    }

    fun deleteDownloadedFolder(downloadFolder: String?) {
        val folder = Paths.get("tempDir", downloadFolder).toFile()
        folder.deleteRecursively()
    }

    private fun setupOutput(projectUrl: String, phase: String, processBuilder: ProcessBuilder) {
        if (propertiesConfiguration.devMode) {
            val repoFileName = Paths.get(
                "tempDir",
                "${projectUrl.substringAfterLast("/")}_${phase}_log.txt"
            ).toFile()

            FileOutputStream(repoFileName)
            processBuilder.redirectOutput(repoFileName)
        } else {
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
    }

    private fun loggingParameter(): String {
        return if (propertiesConfiguration.devMode) {
            "--debug"
        } else {
            "--warn"
        }
    }

    private fun addProxyEnvValues(args: MutableList<String>) {
        if (propertiesConfiguration.httpProxy != "") {
            args.add("-e")
            args.add("http_proxy=${propertiesConfiguration.httpProxy}")
        }

        if (propertiesConfiguration.httpsProxy != "") {
            args.add("-e")
            args.add("https_proxy=${propertiesConfiguration.httpsProxy}")
        }
    }

    private fun addOrtArgs(args: MutableList<String>, ortArgs: MutableList<String>) {
        ortArgs.forEach {
            args.add(it)
        }
    }
}
