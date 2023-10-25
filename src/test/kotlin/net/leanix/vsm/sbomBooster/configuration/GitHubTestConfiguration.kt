package net.leanix.vsm.sbomBooster.configuration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.annotation.PostConstruct
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.service.GitHubApiService
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class GitHubTestConfiguration : GenericTestConfiguration() {
    @MockkBean
    private lateinit var gitHubApiService: GitHubApiService

    @PostConstruct
    fun prepare() {
        every { propertiesConfiguration.githubApiHost } returns "https://api.github.com"
        every { propertiesConfiguration.gitProvider } returns "github"
        every { propertiesConfiguration.githubGraphqlApiUrl } returns "https://api.github.com/graphql"
        every { propertiesConfiguration.githubOrganization } returns "githubOrganization"
        every { propertiesConfiguration.githubToken } returns "githubToken"

        every { gitHubApiService.getUsername("githubToken") } returns "githubUsername"
        every { gitHubApiService.getRepositories("githubToken", "githubOrganization") } returns listOf(
            Repository(
                "cloneUrl1",
                "sourceType",
                "sourceInstance",
                "repositoryName1",
                "repositoryId1"
            ),
            Repository(
                "cloneUrl2",
                "sourceType",
                "sourceInstance",
                "repositoryName2",
                "repositoryId2"
            )
        )

        every {
            ortService.downloadProject(
                any(),
                "githubUsername",
                "githubToken"
            )
        } returns "downloadedFolder"
        every { ortService.analyzeProject(any(), "downloadedFolder") } returns "ortFolder"
    }
}
