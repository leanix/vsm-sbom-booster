package net.leanix.vsm.sbomBooster

import com.ninjasquad.springmockk.MockkBean
import net.leanix.vsm.sbomBooster.service.BitBucketApiService
import net.leanix.vsm.sbomBooster.service.ExitScheduler
import net.leanix.vsm.sbomBooster.service.GitHubApiService
import net.leanix.vsm.sbomBooster.service.GitLabApiService
import net.leanix.vsm.sbomBooster.service.MtMService
import net.leanix.vsm.sbomBooster.service.OrtService
import net.leanix.vsm.sbomBooster.service.SummaryReportService
import net.leanix.vsm.sbomBooster.service.VsmDiscoveryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.ThreadPoolExecutor

@SpringBootTest
class VsmSbomBoosterApplicationTests {
    @MockkBean
    internal lateinit var threadPoolExecutor: ThreadPoolExecutor

    @MockkBean(name = "taskExecutor")
    internal lateinit var taskExecutor: ThreadPoolExecutor

    @MockkBean
    internal lateinit var exitScheduler: ExitScheduler

    @MockkBean(relaxed = true)
    internal lateinit var summaryReportService: SummaryReportService

    @MockkBean
    internal lateinit var vsmDiscoveryService: VsmDiscoveryService

    @MockkBean(relaxed = true)
    internal lateinit var mtMService: MtMService

    @Autowired
    internal lateinit var bitBucketApiService: BitBucketApiService

    @Autowired
    internal lateinit var gitHubApiService: GitHubApiService

    @Autowired
    internal lateinit var gitLabApiService: GitLabApiService

    @Autowired
    internal lateinit var ortService: OrtService
}
