package net.leanix.vsm.sbomBooster.service

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
        username: String?,
        projectUrl: String,
        githubToken: String,
        leanIxToken: String,
        region: String
    ) {
        val startInstant = Instant.now()
        var downloadedFolder: String? = null
        if (!username.isNullOrBlank()) {
            try {
                downloadedFolder = ortService.downloadProject(projectUrl, username, githubToken)
                ortService.analyzeProject(downloadedFolder)
                ortService.generateSbom(downloadedFolder)
                val accessToken = mtMService.getAccessToken(region, leanIxToken)
                vsmDiscoveryService.sendToVsm(projectUrl, downloadedFolder, accessToken!!)
            } catch (e: Exception) {
                logger.error(e.message)
            } finally {
                ortService.deleteDownloadedFolder(downloadedFolder)
            }
        }

        val endInstant = Instant.now()
        logger.info(
            "Processed repository with url: $projectUrl in " +
                "${(endInstant.toEpochMilli() - startInstant.toEpochMilli()).toDuration(DurationUnit.MILLISECONDS)}"
        )
    }
}
