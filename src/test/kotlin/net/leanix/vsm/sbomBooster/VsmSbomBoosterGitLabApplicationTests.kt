package net.leanix.vsm.sbomBooster

import io.mockk.verify
import net.leanix.vsm.sbomBooster.configuration.GitLabTestConfiguration
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

@Import(GitLabTestConfiguration::class)
class VsmSbomBoosterGitLabApplicationTests : VsmSbomBoosterApplicationTests() {
    @Test
    fun `test GitLab integration`() {
        verify(exactly = 1) {
            summaryReportService.appendRecord(
                "GITLAB_GROUP: gitlabGroup\n"
            )
        }
        verify(exactly = 1) { gitLabApiService.getUsername("gitlabToken") }
        verify(exactly = 1) { gitLabApiService.getRepositories("gitlabToken", "gitlabGroup") }
        verify(exactly = 1) {
            ortService.downloadProject(
                "cloneUrl1",
                "gitlabUsername",
                "gitlabToken",
                any()
            )
        }
        verify(exactly = 1) {
            ortService.downloadProject(
                "cloneUrl2",
                "gitlabUsername",
                "gitlabToken",
                any()
            )
        }
        verify(exactly = 1) {
            ortService.analyzeProject(
                "cloneUrl1",
                any()
            )
        }
        verify(exactly = 1) {
            ortService.analyzeProject(
                "cloneUrl2",
                any()
            )
        }
        verify(exactly = 1) { ortService.generateSbom("cloneUrl1") }
        verify(exactly = 1) { ortService.generateSbom("cloneUrl2") }
    }
}
