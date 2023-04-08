package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class OrtService(
    private val propertiesConfiguration: PropertiesConfiguration
) {
    companion object {
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    }
    fun downloadProject(projectUrl: String, username: String, githubToken: String): String {
        val downloadFolder = "downloaded_${List(10) { charPool.random() }.joinToString("")}"

        val downloadProcessBuilder = ProcessBuilder(
            "docker",
            "run", "--rm",
            "-e", "ORT_HTTP_USERNAME=$username",
            "-e", "ORT_HTTP_PASSWORD=$githubToken",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
            "ort",
            "download",
            "--project-url", "$projectUrl",
            "-o", "/project/$downloadFolder"
        )

        downloadProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val downloadProcess = downloadProcessBuilder.start()

        downloadProcess.waitFor()
        downloadProcess.destroy()

        return downloadFolder
    }

    fun analyzeProject(downloadFolder: String) {
        val analyzeProcessBuilder = ProcessBuilder(
            "docker", "run", "--rm",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/$downloadFolder:/downloadedProject",
            "ort",
            "-P", "ort.analyzer.allowDynamicVersions=true",
            "analyze",
            "-i", "/downloadedProject",
            "-o", "/downloadedProject"
        )

        analyzeProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val analyzeProcess = analyzeProcessBuilder.start()

        analyzeProcess.waitFor(30, TimeUnit.MINUTES)
        analyzeProcess.destroyForcibly()
    }

    fun generateSbom(downloadFolder: String) {
        val generateSbomProcessBuilder = ProcessBuilder(
            "docker", "run", "--rm",
            "-v",
            "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}" +
                "/$downloadFolder:/downloadedProject",
            "ort",
            "report",
            "-f", "CycloneDX",
            "-i", "/downloadedProject/analyzer-result.yml",
            "-o", "/downloadedProject",
            "-O", "CycloneDx=output.file.formats=json",
            "-O", "CycloneDx=schema.version=1.4"
        )

        generateSbomProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val generateSbomProcess = generateSbomProcessBuilder.start()

        generateSbomProcess.waitFor()
        generateSbomProcess.destroy()
    }

    fun deleteDownloadedFolder(downloadFolder: String?) {
        val folder = Paths.get("tempDir", downloadFolder).toFile()
        folder.deleteRecursively()
    }
}
