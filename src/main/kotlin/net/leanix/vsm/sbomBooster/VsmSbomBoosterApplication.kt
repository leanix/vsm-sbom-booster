package net.leanix.vsm.sbomBooster

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.service.GitHubApiService
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
    private val processService: ProcessService,
    private val propertiesConfiguration: PropertiesConfiguration,
    private val summaryReportService: SummaryReportService
) : CommandLineRunner {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VsmSbomBoosterApplication::class.java)
        val counter: AtomicInteger = AtomicInteger(0)
    }

    override fun run(vararg args: String?) {
        logger.info("Starting SBOM extraction application.")

        if (propertiesConfiguration.sourceInstance == "") {
            propertiesConfiguration.sourceInstance = propertiesConfiguration.githubOrganization
        }

        summaryReportService.appendRecord("Started VSM SBOM Booster at: ${LocalDateTime.now()}\n")
        summaryReportService.appendRecord(
            "VSM SBOM Booster started with the following parameters " +
                "(secrets are omitted): \n"
        )
        summaryReportService.appendRecord("MOUNTED_VOLUME: ${propertiesConfiguration.mountedVolume}\n")
        summaryReportService.appendRecord(
            "GITHUB_GRAPHQL_API_URL: " +
                "${propertiesConfiguration.githubGraphqlApiUrl}\n"
        )
        summaryReportService.appendRecord("GITHUB_ORGANIZATION: ${propertiesConfiguration.githubOrganization}\n")
        summaryReportService.appendRecord("REGION: ${propertiesConfiguration.host}\n")
        summaryReportService.appendRecord("SOURCE_TYPE: ${propertiesConfiguration.sourceType}\n")
        summaryReportService.appendRecord("SOURCE_INSTANCE: ${propertiesConfiguration.sourceInstance}\n")
        summaryReportService.appendRecord("CONCURRENCY_FACTOR: ${propertiesConfiguration.concurrencyFactor}\n\n")

        val username = gitHubApiService.getUsername(propertiesConfiguration.githubToken)
        val repositories = gitHubApiService.getRepositories(
            propertiesConfiguration.githubToken, propertiesConfiguration.githubOrganization
        )
        logger.info("Discovered ${repositories.size} repositories to process.")

        repositories.forEach {
            processService.processRepository(
                username,
                it,
                propertiesConfiguration.githubToken,
                propertiesConfiguration.leanIxToken,
                propertiesConfiguration.host
            )
        }
    }
}

fun main() {
    runApplication<VsmSbomBoosterApplication>()
}
