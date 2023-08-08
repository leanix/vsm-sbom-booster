package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.domain.VsmDiscoveryItem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
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

    fun initOrt() {
        logger.info("Pulling the latest version of ORT...")
        ortService.pullOrt()
        logger.info("Pulled the latest version of ORT!")
    }

    @Async
    fun processRepository(
        propertiesConfiguration: PropertiesConfiguration,
        username: String,
        token: String,
        repository: Repository
    ) {
        val startInstant = Instant.now()
        var downloadedFolder: String? = null
        if (username.isNotBlank()) {
            try {
                logger.info("Beginning to download repository with url: ${repository.cloneUrl}")
                downloadedFolder = ortService.downloadProject(
                    repository.cloneUrl,
                    username,
                    token
                )
                logger.info(
                    "Finished downloading repository with url: ${repository.cloneUrl} to temp folder: $downloadedFolder"
                )

                logger.info("Beginning to analyze repository with url: ${repository.cloneUrl}")
                ortService.analyzeProject(repository.cloneUrl, downloadedFolder)
                logger.info(
                    "Finished analyzing repository with url: ${repository.cloneUrl} in temp folder $downloadedFolder"
                )

                ortService.generateSbom(repository.cloneUrl, downloadedFolder)
                logger.info(
                    "Finished generating SBOM file for repository with url: " +
                        "${repository.cloneUrl} in temp folder $downloadedFolder."
                )

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
                        downloadedFolder,
                        repository.sourceType,
                        repository.sourceInstance,
                        repository.name,
                        repository.repoId
                    )
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
            "Processed repository with url: ${repository.cloneUrl} in $duration"
        )
    }

    private fun extractVsmRegion(accessToken: String): String {
        val decoder = Base64.getDecoder()
        val payload = String(decoder.decode(accessToken.split(".")[1]))

        return payload.substringAfter("\"iss\":\"https://").substringBefore("-svc.leanix.net")
    }
}
