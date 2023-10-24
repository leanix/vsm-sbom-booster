package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class CycloneDxCliService(
    private val propertiesConfiguration: PropertiesConfiguration
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CycloneDxCliService::class.java)
        private const val CYCLONEDX_IMAGE: String = "cyclonedx/cyclonedx-cli"
    }

    fun pullCdxCli() {
        val pullCdxCliProcessBuilder = ProcessBuilder(
            "docker",
            "pull",
            CYCLONEDX_IMAGE
        )

        pullCdxCliProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        val downloadProcess = pullCdxCliProcessBuilder.start()

        downloadProcess.waitFor(15, TimeUnit.MINUTES)
        downloadProcess.destroy()
    }

    fun mergeSboms(
        file1: String,
        file2: String,
        outputFolder: String
    ) {
        val args = mutableListOf(
            "docker",
            "run", "--rm",
            "-v", "${Paths.get(propertiesConfiguration.mountedVolume).toAbsolutePath()}:/project",
        )

        val ortArgs = mutableListOf(
            CYCLONEDX_IMAGE,
            "merge",
            "--input-files", "/project/$file1", "/project/$file2",
            "--output-file", "/project/$outputFolder/bom.cyclonedx.json"
        )
        addArgs(args, ortArgs)

        logger.info("Running merge command with the following arguments: $args")

        val processBuilder = ProcessBuilder(args)

        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = processBuilder.start()

        process.waitFor(15, TimeUnit.MINUTES)
        process.destroy()
    }

    private fun addArgs(args: MutableList<String>, ortArgs: MutableList<String>) {
        ortArgs.forEach {
            args.add(it)
        }
    }
}
