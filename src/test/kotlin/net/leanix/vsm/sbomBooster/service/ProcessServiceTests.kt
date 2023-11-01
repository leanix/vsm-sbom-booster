package net.leanix.vsm.sbomBooster.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.domain.VsmDiscoveryItem
import net.leanix.vsm.sbomBooster.domain.VsmSbomBoosterUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProcessServiceTests {
    private val propertiesConfiguration: PropertiesConfiguration = mockk()
    private val vsmDiscoveryService: VsmDiscoveryService = mockk()
    private val mtMService: MtMService = mockk()
    private val ortService: OrtService = mockk()
    private val cycloneDxCliService: CycloneDxCliService = mockk()
    private val sbomBuilderService: SbomBuilderService = mockk()
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
        every { propertiesConfiguration.gitProvider } returns "GITHUB"
        every { propertiesConfiguration.githubDependencyGraph } returns true

        mockkObject(VsmSbomBoosterUtils)
        every { VsmSbomBoosterUtils.deleteFolder(any()) } returns Unit
        every { VsmSbomBoosterUtils.writeStringToFile(any(), any(), any()) } returns Unit

        every {
            sbomBuilderService.fromGithubDependencyGraph(propertiesConfiguration, "token", repository, any())
        } returns true

        every {
            ortService.downloadProject(
                repository.cloneUrl,
                username,
                token,
                any()
            )
        } returns "downloadedFolder"

        every { ortService.analyzeProject(repository.cloneUrl, any()) } returns "ortFolder"

        every { ortService.generateSbom(repository.cloneUrl) } returns Unit

        every { cycloneDxCliService.mergeSboms(any(), any(), any()) } returns Unit

        every {
            mtMService.getAccessToken(
                propertiesConfiguration.leanIxHost,
                propertiesConfiguration.leanIxToken
            )
        } returns accessToken

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
        val processService = ProcessService(
            ortService,
            cycloneDxCliService,
            vsmDiscoveryService,
            mtMService,
            sbomBuilderService,
            propertiesConfiguration
        )
        processService.processRepository(username, token, repository)

        verify(exactly = 1) {
            vsmDiscoveryService.sendToVsm(
                accessToken,
                "eu",
                VsmDiscoveryItem(
                    repository.cloneUrl,
                    "cloneUrl_merged_sbom",
                    repository.sourceType,
                    repository.sourceInstance,
                    repository.name,
                    repository.repoId
                )
            )
        }
        verify(exactly = 1) { VsmSbomBoosterUtils.deleteFolder(match { it.matches("cloneUrl_\\w{10}".toRegex()) }) }
        verify(exactly = 1) { VsmSbomBoosterUtils.deleteFolder("cloneUrl_merged_sbom",) }
        verify(exactly = 1) { VsmSbomBoosterUtils.deleteFolder("cloneUrl_GIT_Provided_SBOM") }
        verify(exactly = 1) { VsmSbomBoosterUtils.deleteFolder("cloneUrl_merged_sbom") }
    }

    @Test
    fun `processRepository function calls the correct sequence of functions when devMode is enabled`() {
        every { propertiesConfiguration.devMode } returns true
        val processService = ProcessService(
            ortService,
            cycloneDxCliService,
            vsmDiscoveryService,
            mtMService,
            sbomBuilderService,
            propertiesConfiguration
        )
        processService.processRepository(username, token, repository)

        verify(exactly = 1) {
            vsmDiscoveryService.sendToVsm(
                accessToken,
                "eu",
                VsmDiscoveryItem(
                    repository.cloneUrl,
                    "cloneUrl_merged_sbom",
                    repository.sourceType,
                    repository.sourceInstance,
                    repository.name,
                    repository.repoId
                )
            )
        }
        verify(exactly = 1) { VsmSbomBoosterUtils.deleteFolder(match { it.matches("cloneUrl_\\w{10}".toRegex()) }) }
        verify(exactly = 0) { VsmSbomBoosterUtils.deleteFolder("ortFolder") }
        verify(exactly = 0) { VsmSbomBoosterUtils.deleteFolder("downloadedFolder_GIT_Provided_SBOM") }
        verify(exactly = 0) { VsmSbomBoosterUtils.deleteFolder("downloadedFolder_merged_sbom") }
    }
}
