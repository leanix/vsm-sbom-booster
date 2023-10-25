package net.leanix.vsm.sbomBooster

import io.mockk.verify
import net.leanix.vsm.sbomBooster.configuration.GitHubTestConfiguration
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

@Import(GitHubTestConfiguration::class)
class VsmSbomBoosterGitHubApplicationTests : VsmSbomBoosterApplicationTests() {
    @Test
    fun `test GitHub integration`() {
        verify(exactly = 1) {
            summaryReportService.appendRecord(
                "GITHUB_ORGANIZATION: githubOrganization\n"
            )
        }
        verify(exactly = 1) { gitHubApiService.getUsername("githubToken") }
        verify(exactly = 1) { gitHubApiService.getRepositories("githubToken", "githubOrganization") }
        verify(exactly = 1) {
            ortService.downloadProject(
                "cloneUrl1",
                "githubUsername",
                "githubToken",
            )
        }
        verify(exactly = 1) {
            ortService.downloadProject(
                "cloneUrl2",
                "githubUsername",
                "githubToken",
            )
        }
        verify(exactly = 1) {
            ortService.analyzeProject(
                "cloneUrl1",
                "downloadedFolder"
            )
        }
        verify(exactly = 1) {
            ortService.analyzeProject(
                "cloneUrl2",
                "downloadedFolder"
            )
        }
        verify(exactly = 1) { ortService.generateSbom("cloneUrl1") }
        verify(exactly = 1) { ortService.generateSbom("cloneUrl2") }
    }
}
