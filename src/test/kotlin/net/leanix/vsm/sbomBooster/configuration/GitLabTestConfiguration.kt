package net.leanix.vsm.sbomBooster.configuration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.annotation.PostConstruct
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.service.GitLabApiService
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class GitLabTestConfiguration : GenericTestConfiguration() {

    @MockkBean
    private lateinit var gitLabApiService: GitLabApiService

    @PostConstruct
    fun prepare() {
        every { propertiesConfiguration.gitlabApiHost } returns "https://gitlab.com"
        every { propertiesConfiguration.gitProvider } returns "gitlab"
        every { propertiesConfiguration.gitlabGraphqlApiUrl } returns "https://gitlab.com/api/graphql"
        every { propertiesConfiguration.gitlabGroup } returns "gitlabGroup"
        every { propertiesConfiguration.gitlabToken } returns "gitlabToken"

        every { gitLabApiService.getUsername("gitlabToken") } returns "gitlabUsername"
        every { gitLabApiService.getRepositories("gitlabToken", "gitlabGroup") } returns listOf(
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
                "gitlabUsername",
                "gitlabToken"
            )
        } returns "downloadedFolder"
        every { ortService.analyzeProject(any(), "downloadedFolder") } returns "ortFolder"
    }
}
