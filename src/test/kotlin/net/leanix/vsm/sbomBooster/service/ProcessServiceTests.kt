package net.leanix.vsm.sbomBooster.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.domain.VsmDiscoveryItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProcessServiceTests {
    private val propertiesConfiguration: PropertiesConfiguration = mockk()
    private val vsmDiscoveryService: VsmDiscoveryService = mockk()
    private val mtMService: MtMService = mockk()
    private val ortService: OrtService = mockk()
    private lateinit var repository: Repository
    private lateinit var username: String
    private lateinit var token: String
    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        repository = Repository(
            "cloneUrl",
            "sourceType",
            "sourceInstance",
            "repositoryName",
            "repositoryId"
        )
        username = "repository"
        token = "token"
        accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkdW1teUBsZWFuaXgubmV0IiwiaXNzIjoiaHR0cHM6Ly9ldS1zdmMubGVhbml" +
            "4Lm5ldCIsImV4cCI6MTY5ODA4MTI3MiwiaW5zdGFuY2VVcmwiOiJodHRwczovL2V1LmxlYW5peC5uZXQiLCJyZWdpb24iOiJ3Z" +
            "XN0ZXVyb3BlIn0.6Dyq33zVUmpp3AHUmwPTWuOwGQIxpybvUXhiQNeq-_E"

        every { propertiesConfiguration.leanIxHost } returns "leanIxHost"
        every { propertiesConfiguration.leanIxToken } returns "leanIxToken"

        every {
            ortService.downloadProject(
                repository.cloneUrl,
                username,
                token
            )
        } returns "downloadedFolder"

        every { ortService.analyzeProject(repository.cloneUrl, "downloadedFolder") } returns "ortFolder"

        every { ortService.generateSbom(repository.cloneUrl) } returns Unit

        every {
            mtMService.getAccessToken(
                propertiesConfiguration.leanIxHost,
                propertiesConfiguration.leanIxToken
            )
        } returns accessToken

        every { ortService.deleteDownloadedFolder("downloadedFolder") } returns Unit
        every { ortService.deleteDownloadedFolder("ortFolder") } returns Unit

        every { mtMService.getAccessToken("leanIxHost", "leanIxToken") } returns accessToken

        every {
            vsmDiscoveryService.sendToVsm(
                accessToken,
                "eu",
                VsmDiscoveryItem(
                    repository.cloneUrl,
                    "ortFolder",
                    repository.sourceType,
                    repository.sourceInstance,
                    repository.name,
                    repository.repoId
                )
            )
        } returns Unit
    }

    @Test
    fun `processRepository function calls the correct sequence of functions when devMode is disabled`() {
        every { propertiesConfiguration.devMode } returns false
        val processService = ProcessService(ortService, vsmDiscoveryService, mtMService)
        processService.processRepository(propertiesConfiguration, username, token, repository)

        verify(exactly = 1) {
            vsmDiscoveryService.sendToVsm(
                accessToken,
                "eu",
                VsmDiscoveryItem(
                    repository.cloneUrl,
                    "ortFolder",
                    repository.sourceType,
                    repository.sourceInstance,
                    repository.name,
                    repository.repoId
                )
            )
        }
        verify(exactly = 1) { ortService.deleteDownloadedFolder("downloadedFolder") }
        verify(exactly = 1) { ortService.deleteDownloadedFolder("ortFolder") }
    }

    @Test
    fun `processRepository function calls the correct sequence of functions when devMode is enabled`() {
        every { propertiesConfiguration.devMode } returns true
        val processService = ProcessService(ortService, vsmDiscoveryService, mtMService)
        processService.processRepository(propertiesConfiguration, username, token, repository)

        verify(exactly = 1) {
            vsmDiscoveryService.sendToVsm(
                accessToken,
                "eu",
                VsmDiscoveryItem(
                    repository.cloneUrl,
                    "ortFolder",
                    repository.sourceType,
                    repository.sourceInstance,
                    repository.name,
                    repository.repoId
                )
            )
        }
        verify(exactly = 1) { ortService.deleteDownloadedFolder("downloadedFolder") }
        verify(exactly = 0) { ortService.deleteDownloadedFolder("ortFolder") }
    }
}
