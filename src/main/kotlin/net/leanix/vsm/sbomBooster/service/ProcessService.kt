package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class ProcessService(
    private val ortService: OrtService,
    private val vsmDiscoveryService: VsmDiscoveryService,
    private val mtMService: MtMService
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProcessService::class.java)
    }

    @Async
    fun processRepository(
        propertiesConfiguration: PropertiesConfiguration,
        username: String?,
        projectUrl: String
    ) {
        val startInstant = Instant.now()
        var downloadedFolder: String? = null
        if (!username.isNullOrBlank()) {
            try {
                logger.info("Beginning to download repository with url: $projectUrl")
                downloadedFolder = ortService.downloadProject(projectUrl, username, propertiesConfiguration.githubToken)
                logger.info("Finished downloading repository with url: $projectUrl to temp folder: $downloadedFolder")

                logger.info("Beginning to analyze repository with url: $projectUrl")
                ortService.analyzeProject(downloadedFolder)
                logger.info("Finished analyzing repository with url: $projectUrl in temp folder $downloadedFolder")

                ortService.generateSbom(downloadedFolder)
                logger.info(
                    "Finished generating SBOM file for repository with url: " +
                        "$projectUrl in temp folder $downloadedFolder."
                )

                val accessToken = mtMService.getAccessToken(
                    propertiesConfiguration.host,
                    propertiesConfiguration.leanIxToken
                )
                vsmDiscoveryService.sendToVsm(
                    projectUrl, downloadedFolder, accessToken!!,
                    propertiesConfiguration.region
                )
            } catch (e: Exception) {
                logger.error(e.message)
            } finally {
                ortService.deleteDownloadedFolder(downloadedFolder)
                logger.info("Finished deleting temp folder $downloadedFolder.")
            }
        }

        val duration = (
            Instant.now().toEpochMilli() -
                startInstant.toEpochMilli()
            ).toDuration(DurationUnit.MILLISECONDS)

        logger.info(
            "Processed repository with url: $projectUrl in $duration"
        )
    }
}
