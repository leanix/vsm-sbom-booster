package net.leanix.vsm.sbomBooster

import io.mockk.verify
import net.leanix.vsm.sbomBooster.configuration.BitBucketTestConfiguration
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

@Import(BitBucketTestConfiguration::class)
class VsmSbomBoosterBitBucketApplicationTests : VsmSbomBoosterApplicationTests() {

    @Test
    fun `test BitBucket integration`() {
        verify(exactly = 1) {
            summaryReportService.appendRecord(
                "BITBUCKET_WORKSPACE: bitbucketWorkspace\n"
            )
        }
        verify(exactly = 1) { bitBucketApiService.getUsername("") }
        verify(exactly = 1) {
            bitBucketApiService
                .getRepositories("bitbucketKey:bitbucketSecret", "bitbucketWorkspace")
        }
        verify(exactly = 1) {
            ortService.downloadProject(
                "cloneUrl1",
                "bitbucketUsername",
                "bitbucketToken",
                any()
            )
        }
        verify(exactly = 1) {
            ortService.downloadProject(
                "cloneUrl2",
                "bitbucketUsername",
                "bitbucketToken",
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
