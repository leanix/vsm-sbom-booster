package net.leanix.vsm.sbomBooster

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.service.GitHubApiService
import net.leanix.vsm.sbomBooster.service.ProcessService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
@EnableConfigurationProperties(PropertiesConfiguration::class)
class VsmSbomBoosterApplication(
    private val gitHubApiService: GitHubApiService,
    private val processService: ProcessService,
    private val propertiesConfiguration: PropertiesConfiguration
) : CommandLineRunner {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VsmSbomBoosterApplication::class.java)
        val counter: AtomicInteger = AtomicInteger(0)
    }

    override fun run(vararg args: String?) {
        logger.info("Starting SBOM extraction application.")
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
                propertiesConfiguration.region
            )
        }
    }
}

fun main() {
    runApplication<VsmSbomBoosterApplication>()
}
