package net.leanix.vsm.sbomBooster.service

import kotlinx.coroutines.runBlocking
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.domain.VSMDiscoveryItem
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
        private var initStatus: Boolean = false
    }

    fun initOrt() {
        runBlocking {
            if (!initStatus) {
                ortService.pullOrt()
                initStatus = true
            }
        }
    }

    @Async
    fun processRepository(
        propertiesConfiguration: PropertiesConfiguration,
        username: String?,
        repository: Repository
    ) {
        initOrt()

        val startInstant = Instant.now()
        var downloadedFolder: String? = null
        if (!username.isNullOrBlank()) {
            try {
                logger.info("Beginning to download repository with url: ${repository.cloneUrl}")
                downloadedFolder = ortService.downloadProject(
                    repository.cloneUrl,
                    username,
                    propertiesConfiguration.githubToken
                )
                logger.info(
                    "Finished downloading repository with url: ${repository.cloneUrl} to temp folder: $downloadedFolder"
                )

                logger.info("Beginning to analyze repository with url: ${repository.cloneUrl}")
                ortService.analyzeProject(downloadedFolder)
                logger.info(
                    "Finished analyzing repository with url: ${repository.cloneUrl} in temp folder $downloadedFolder"
                )

                ortService.generateSbom(downloadedFolder)
                logger.info(
                    "Finished generating SBOM file for repository with url: " +
                        "${repository.cloneUrl} in temp folder $downloadedFolder."
                )

                val accessToken = mtMService.getAccessToken(
                    propertiesConfiguration.leanIxHost,
                    propertiesConfiguration.leanIxToken
                )
                vsmDiscoveryService.sendToVsm(
                    accessToken!!,
                    propertiesConfiguration.leanIxRegion,
                    VSMDiscoveryItem(
                        repository.cloneUrl,
                        downloadedFolder,
                        repository.sourceType,
                        repository.sourceInstance,
                        repository.name
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
}
