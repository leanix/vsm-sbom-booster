package net.leanix.vsm.sbomBooster.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.VsmDiscoveryItem
import org.cyclonedx.BomParserFactory
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Component
import org.cyclonedx.parsers.Parser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.nio.file.Files

class VsmDiscoveryServiceTests {
    private val summaryReportService: SummaryReportService = mockk<SummaryReportService>(relaxed = true)
    private val propertiesConfiguration: PropertiesConfiguration = mockk()
    private val restTemplate: RestTemplate = mockk()
    private val bom: Bom = mockk()
    private val parser: Parser = mockk()
    private val vsmDiscoveryItem = VsmDiscoveryItem(
        "projectUrl",
        "ortFolder",
        "sourceType",
        "sourceInstance",
        "name",
        "repoId"
    )

    @BeforeEach
    fun setUp() {
        every { restTemplate.postForEntity(ofType(String::class), any(), String::class.java) } returns ResponseEntity(
            "{}", HttpStatus.OK
        )

        mockkStatic(Files::class)
        every { Files.readAllBytes(any()) } returns ByteArray(0)

        mockkStatic(BomParserFactory::class)
        every { BomParserFactory.createParser(ByteArray(0)) } returns parser

        every { parser.parse(ByteArray(0)) } returns bom
    }

    @Test
    fun `When allowNoComponentSboms is true sbom is submitted when components are present`() {
        every { propertiesConfiguration.allowNoComponentSboms } returns true
        val vsmDiscoveryService = VsmDiscoveryService(summaryReportService, propertiesConfiguration, restTemplate)

        every { bom.components } returns listOf(
            Component().apply {
                name = "componentName"
                version = "componentVersion"
            }
        )

        vsmDiscoveryService.sendToVsm("token", "region", vsmDiscoveryItem)

        verify(exactly = 1) {
            summaryReportService.appendRecord("Successfully processed repository with url: projectUrl \n")
        }
    }

    @Test
    fun `When allowNoComponentSboms is true sbom is submitted when components are not present`() {
        every { propertiesConfiguration.allowNoComponentSboms } returns true
        val vsmDiscoveryService = VsmDiscoveryService(summaryReportService, propertiesConfiguration, restTemplate)

        every { bom.components } returns listOf()

        vsmDiscoveryService.sendToVsm("token", "region", vsmDiscoveryItem)

        verify(exactly = 1) {
            summaryReportService
                .appendRecord("Successfully processed repository with url: projectUrl with zero components\n")
        }
    }

    @Test
    fun `When allowNoComponentSboms is false sbom is submitted when it has components`() {
        every { propertiesConfiguration.allowNoComponentSboms } returns false
        val vsmDiscoveryService = VsmDiscoveryService(summaryReportService, propertiesConfiguration, restTemplate)

        every { bom.components } returns listOf(
            Component().apply {
                name = "componentName"
                version = "componentVersion"
            }
        )

        vsmDiscoveryService.sendToVsm("token", "region", vsmDiscoveryItem)

        verify(exactly = 1) {
            summaryReportService.appendRecord("Successfully processed repository with url: projectUrl \n")
        }
    }

    @Test
    fun `When allowNoComponentSboms is false sbom is not submitted when it has no components`() {
        every { propertiesConfiguration.allowNoComponentSboms } returns false
        val vsmDiscoveryService = VsmDiscoveryService(summaryReportService, propertiesConfiguration, restTemplate)

        every { bom.components } returns listOf()

        vsmDiscoveryService.sendToVsm("token", "region", vsmDiscoveryItem)

        verify(exactly = 1) {
            summaryReportService.appendRecord("Failed to process repository with url: projectUrl \n")
        }
    }
}
