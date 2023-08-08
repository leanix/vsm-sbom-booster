package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
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
        val downloadFolder = "downloaded_${List(10) { charPool.random() }.joinToString("")}"

        val downloadProcessBuilder = ProcessBuilder(
            "docker",
            "run", "--rm",
            "-e", "ORT_HTTP_USERNAME=$username",
            "-e", "ORT_HTTP_PASSWORD=$gitToken",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
            "leanixacrpublic.azurecr.io/ort",
            "download",
            "--project-url", projectUrl,
            "-o", "/project/$downloadFolder"
        )

        setupOutput(projectUrl, "download", downloadProcessBuilder)

        val downloadProcess = downloadProcessBuilder.start()

        downloadProcess.waitFor(15, TimeUnit.MINUTES)
        downloadProcess.destroy()

        return downloadFolder
    }

    fun analyzeProject(projectUrl: String, downloadFolder: String) {
        val analyzeProcessBuilder = ProcessBuilder(
            "docker", "run", "--rm",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/$downloadFolder:/downloadedProject",
            "leanixacrpublic.azurecr.io/ort",
            "-P", "ort.analyzer.allowDynamicVersions=true",
            "analyze",
            "-i", "/downloadedProject",
            "-o", "/downloadedProject"
        )

        setupOutput(projectUrl, "analyze", analyzeProcessBuilder)

        val analyzeProcess = analyzeProcessBuilder.start()

        analyzeProcess.waitFor(propertiesConfiguration.analysisTimeout, TimeUnit.MINUTES)
        analyzeProcess.destroy()
    }

    fun generateSbom(projectUrl: String, downloadFolder: String) {
        val generateSbomProcessBuilder = ProcessBuilder(
            "docker", "run", "--rm",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/$downloadFolder:/downloadedProject",
            "leanixacrpublic.azurecr.io/ort",
            "report",
            "-f", "CycloneDX",
            "-i", "/downloadedProject/analyzer-result.yml",
            "-o", "/downloadedProject",
            "-O", "CycloneDx=output.file.formats=json",
            "-O", "CycloneDx=schema.version=1.4"
        )

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
                "${projectUrl.substringAfterLast("/")}_$phase.txt"
            ).toFile()

            FileOutputStream(repoFileName)
            processBuilder.redirectOutput(repoFileName)
        } else {
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
    }
}
