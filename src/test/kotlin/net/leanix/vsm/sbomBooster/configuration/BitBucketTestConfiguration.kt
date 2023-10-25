package net.leanix.vsm.sbomBooster.configuration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.annotation.PostConstruct
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.service.BitBucketApiService
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class BitBucketTestConfiguration : GenericTestConfiguration() {

    @MockkBean
    private lateinit var bitBucketApiService: BitBucketApiService

    @PostConstruct
    fun prepare() {
        every { propertiesConfiguration.bitbucketKey } returns "bitbucketKey"
        every { propertiesConfiguration.gitProvider } returns "bitbucket"
        every { propertiesConfiguration.bitbucketSecret } returns "bitbucketSecret"
        every { propertiesConfiguration.bitbucketWorkspace } returns "bitbucketWorkspace"
        every { propertiesConfiguration.bitbucketKey } returns "bitbucketKey"

        every { bitBucketApiService.getUsername("") } returns "bitbucketUsername"
        every { bitBucketApiService.authenticate("bitbucketKey:bitbucketSecret") } returns "bitbucketToken"
        every {
            bitBucketApiService
                .getRepositories("bitbucketKey:bitbucketSecret", "bitbucketWorkspace")
        } returns listOf(
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
                "bitbucketUsername",
                "bitbucketToken"
            )
        } returns "downloadedFolder"
        every { ortService.analyzeProject(any(), "downloadedFolder") } returns "ortFolder"
    }
}
