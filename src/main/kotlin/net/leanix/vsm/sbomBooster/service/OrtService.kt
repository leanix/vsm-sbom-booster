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
        private val logger: Logger = LoggerFactory.getLogger(OrtService::class.java)
    }

    fun pullOrt() {
        val pullOrtProcessBuilder = ProcessBuilder(
            "docker",
            "pull",
            propertiesConfiguration.ortImage
        )

        pullOrtProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val downloadProcess = pullOrtProcessBuilder.start()

        downloadProcess.waitFor(15, TimeUnit.MINUTES)
        downloadProcess.destroy()
    }

    fun downloadProject(projectUrl: String, username: String, gitToken: String, downloadFolder: String): String {
        val args = mutableListOf(
            "docker",
            "run", "--rm",
            "-e", "ORT_HTTP_USERNAME=$username",
            "-e", "ORT_HTTP_PASSWORD=$gitToken",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
            "-e", "ORT_CONFIG_DIR=/project/config"
        )

        addProxyEnvValues(args)
        val ortArgs = mutableListOf(
            propertiesConfiguration.ortImage,
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
            "docker", "run", "--rm",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
            "-e", "ORT_CONFIG_DIR=/project/config",
        )

        addProxyEnvValues(args)

        val ortArgs = mutableListOf(
            propertiesConfiguration.ortImage,
            loggingParameter(),
            "analyze",
            "-i", "/project/$downloadFolder",
            "-o", "/project/$ortFolder",
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
            "docker", "run", "--rm",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
            "-e", "ORT_CONFIG_DIR=/project/config",
        )

        addProxyEnvValues(args)
        val ortArgs = mutableListOf(
            propertiesConfiguration.ortImage,
            loggingParameter(),
            "report",
            "-f", "CycloneDX",
            "-i", "/project/${projectUrl.substringAfterLast("/")}_ORT_produced_files/analyzer-result.yml",
            "-o", "/project/${projectUrl.substringAfterLast("/")}_ORT_produced_files",
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
