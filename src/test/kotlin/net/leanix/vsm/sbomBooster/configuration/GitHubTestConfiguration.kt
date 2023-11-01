package net.leanix.vsm.sbomBooster.configuration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.annotation.PostConstruct
import net.leanix.vsm.sbomBooster.domain.Package
import net.leanix.vsm.sbomBooster.domain.PackageManager
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
        every { propertiesConfiguration.githubDependencyGraph } returns true

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
            gitHubApiService.getAllDependencies(
                "githubToken",
                "githubOrganization",
                any()
            )
        } returns mutableMapOf(
            Pair(
                "packageManager1",
                PackageManager(
                    "packageManager1",
                    mutableMapOf(
                        Pair(
                            "package1",
                            Package(
                                "package1",
                                mutableMapOf(
                                    Pair(
                                        "1.0.0",
                                        true
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            Pair(
                "packageManager2",
                PackageManager(
                    "packageManager2",
                    mutableMapOf(
                        Pair(
                            "package1",
                            Package(
                                "package1",
                                mutableMapOf(
                                    Pair(
                                        "1.0.0",
                                        true
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        every {
            ortService.downloadProject(
                any(),
                "githubUsername",
                "githubToken",
                any()
            )
        } returns "downloadedFolder"
        every { ortService.analyzeProject(any(), "downloadedFolder") } returns "ortFolder"
    }
}
