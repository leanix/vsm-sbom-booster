package net.leanix.vsm.sbomBooster

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.GitCredentials
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.service.BitBucketApiService
import net.leanix.vsm.sbomBooster.service.GitHubApiService
import net.leanix.vsm.sbomBooster.service.GitLabApiService
import net.leanix.vsm.sbomBooster.service.ProcessService
import net.leanix.vsm.sbomBooster.service.SummaryReportService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
@EnableConfigurationProperties(PropertiesConfiguration::class)
class VsmSbomBoosterApplication(
    private val gitHubApiService: GitHubApiService,
    private val gitLabApiService: GitLabApiService,
    private val bitBucketApiService: BitBucketApiService,
    private val processService: ProcessService,
    private val propertiesConfiguration: PropertiesConfiguration,
    private val summaryReportService: SummaryReportService
) : CommandLineRunner {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VsmSbomBoosterApplication::class.java)
        val counter: AtomicInteger = AtomicInteger(0)
        var gotRepositories: Boolean = false
    }

    override fun run(vararg args: String?) {
        logger.info("Starting SBOM extraction application.")

        summaryReportService.appendRecord("Started VSM SBOM Booster at: ${LocalDateTime.now()}\n")
        summaryReportService.appendRecord(
            "VSM SBOM Booster started with the following parameters " +
                "(secrets are omitted): \n"
        )
        summaryReportService.appendRecord("MOUNTED_VOLUME: ${propertiesConfiguration.mountedVolume}\n")
        summaryReportService.appendRecord("CONCURRENCY_FACTOR: ${propertiesConfiguration.concurrencyFactor}\n\n")

        summaryReportService.appendRecord("LEANIX_HOST: ${propertiesConfiguration.leanIxHost}\n")

        summaryReportService.appendRecord("SOURCE_TYPE: ${propertiesConfiguration.sourceType}\n")
        summaryReportService.appendRecord("SOURCE_INSTANCE: ${propertiesConfiguration.sourceInstance}\n")

        summaryReportService.appendRecord("GIT_PROVIDER: ${propertiesConfiguration.gitProvider.uppercase()}\n")

        val (credentials, repositories) = getRepositories()

        logger.info("Discovered ${repositories.size} repositories to process.")

        processService.initOrt()

        repositories.forEach {
            processService.processRepository(
                propertiesConfiguration,
                credentials.username,
                credentials.token,
                it
            )
            gotRepositories = true
        }
    }

    fun getRepositories(): Pair<GitCredentials, List<Repository>> {
        val repositories: List<Repository>
        val username: String?
        val token: String?
        when (propertiesConfiguration.gitProvider.uppercase()) {
            "GITHUB" -> {
                summaryReportService.appendRecord(
                    "GITHUB_GRAPHQL_API_URL: " +
                        "${propertiesConfiguration.githubGraphqlApiUrl}\n"
                )
                summaryReportService.appendRecord(
                    "GITHUB_ORGANIZATION: ${propertiesConfiguration.githubOrganization}\n"
                )

                username = gitHubApiService.getUsername(propertiesConfiguration.githubToken)
                token = propertiesConfiguration.githubToken
                repositories = gitHubApiService.getRepositories(
                    propertiesConfiguration.githubToken, propertiesConfiguration.githubOrganization
                )
            }
            "GITLAB" -> {
                summaryReportService.appendRecord(
                    "GITLAB_GRAPHQL_API_URL: " +
                        "${propertiesConfiguration.gitlabGraphqlApiUrl}\n"
                )
                summaryReportService.appendRecord("GITLAB_GROUP: ${propertiesConfiguration.gitlabGroup}\n")

                username = gitLabApiService.getUsername(propertiesConfiguration.gitlabToken)
                token = propertiesConfiguration.gitlabToken
                repositories = gitLabApiService.getRepositories(
                    propertiesConfiguration.gitlabToken, propertiesConfiguration.gitlabGroup
                )
            }
            "BITBUCKET" -> {
                summaryReportService.appendRecord(
                    "BITBUCKET_WORKSPACE: ${propertiesConfiguration.bitbucketWorkspace}\n"
                )

                val bitBucketToken =
                    "${propertiesConfiguration.bitbucketKey}:${propertiesConfiguration.bitbucketSecret}"

                username = bitBucketApiService.getUsername("")
                token = bitBucketApiService.authenticate(bitBucketToken)
                repositories = bitBucketApiService.getRepositories(
                    bitBucketToken, propertiesConfiguration.bitbucketWorkspace
                )
            }
            else -> {
                summaryReportService.appendRecord(
                    "WARNING: INVALID GIT_PROVIDER: ${propertiesConfiguration.gitProvider.uppercase()}\n"
                )
                throw IllegalArgumentException(
                    "Invalid GIT_PROVIDER: ${propertiesConfiguration.gitProvider.uppercase()}"
                )
            }
        }
        return Pair(GitCredentials(username ?: "", token ?: ""), repositories)
    }
}

fun main() {
    runApplication<VsmSbomBoosterApplication>()
}
