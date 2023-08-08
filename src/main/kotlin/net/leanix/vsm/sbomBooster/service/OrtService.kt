package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class OrtService(
    private val propertiesConfiguration: PropertiesConfiguration
) {
    companion object {
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        private fun generateFile(projectUrl: String, phase: String): File {
            return Paths.get(
                "tempDir",
                "${projectUrl.substringAfterLast("/")}_$phase.txt"
            ).toFile()
        }
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

        if (propertiesConfiguration.devMode) {
            val repoFileName = generateFile(projectUrl, "download")

            FileOutputStream(repoFileName)
            downloadProcessBuilder.redirectOutput(repoFileName)
        } else {
            downloadProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        }

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

        if (propertiesConfiguration.devMode) {
            val repoFileName = generateFile(projectUrl, "analyze")

            FileOutputStream(repoFileName)
            analyzeProcessBuilder.redirectOutput(repoFileName)
        } else {
            analyzeProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        }

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

        if (propertiesConfiguration.devMode) {
            val repoFileName = generateFile(projectUrl, "generate_sbom")

            FileOutputStream(repoFileName)
            generateSbomProcessBuilder.redirectOutput(repoFileName)
        } else {
            generateSbomProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        }

        val generateSbomProcess = generateSbomProcessBuilder.start()

        generateSbomProcess.waitFor(10, TimeUnit.MINUTES)
        generateSbomProcess.destroy()
    }

    fun deleteDownloadedFolder(downloadFolder: String?) {
        val folder = Paths.get("tempDir", downloadFolder).toFile()
        folder.deleteRecursively()
    }
}
