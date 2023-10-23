package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.VsmSbomBoosterApplication
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.VsmDiscoveryItem
import org.cyclonedx.BomParserFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

@Service
class VsmDiscoveryService(
    private val summaryReportService: SummaryReportService,
    private val propertiesConfiguration: PropertiesConfiguration,
    private val restTemplate: RestTemplate
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VsmDiscoveryService::class.java)
        const val SOURCE_CUSTOM_HEADER = "X-Lx-Vsm-Discovery-Source"
    }

    fun sendToVsm(
        leanIxToken: String,
        region: String,
        discoveryItem: VsmDiscoveryItem
    ) {
        val containsComponents = bomContainsComponents(discoveryItem)

        if (propertiesConfiguration.allowNoComponentSboms) {
            submitSbom(leanIxToken, discoveryItem, region, containsComponents)
        } else {
            submitSbomBasedOnComponents(discoveryItem, leanIxToken, region, containsComponents)
        }
    }

    private fun submitSbomBasedOnComponents(
        discoveryItem: VsmDiscoveryItem,
        leanIxToken: String,
        region: String,
        containsComponents: Boolean
    ) {
        if (containsComponents) {
            submitSbom(leanIxToken, discoveryItem, region, containsComponents)
        } else {
            logger.info("No components found in the SBOM file for repository ${discoveryItem.projectUrl}")
            summaryReportService.appendRecord("Failed to process repository with url: ${discoveryItem.projectUrl} \n")
        }
    }

    private fun bomContainsComponents(discoveryItem: VsmDiscoveryItem): Boolean {
        val sbomByteArray = Files.readAllBytes(
            Paths.get(
                "tempDir",
                discoveryItem.ortFolder,
                "bom.cyclonedx.json"
            )
        )

        val parser = BomParserFactory.createParser(sbomByteArray)

        val bom = parser.parse(sbomByteArray)
        return !bom.components.isNullOrEmpty()
    }

    private fun submitSbom(
        leanIxToken: String,
        discoveryItem: VsmDiscoveryItem,
        region: String,
        containsComponents: Boolean
    ) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.set("Authorization", "Bearer $leanIxToken")
        headers.set(SOURCE_CUSTOM_HEADER, "vsm-sbom-booster")

        val multipartBodyBuilder = MultipartBodyBuilder()

        multipartBodyBuilder.part("id", discoveryItem.name)
        multipartBodyBuilder.part("repoId", discoveryItem.repoId)
        multipartBodyBuilder.part("sourceType", discoveryItem.sourceType)
        multipartBodyBuilder.part("sourceInstance", discoveryItem.sourceInstance)
        multipartBodyBuilder.part("name", discoveryItem.name)

        val sbomFile: Resource = FileSystemResource(
            "${Paths.get("tempDir").toAbsolutePath()}" +
                "/${discoveryItem.ortFolder}/bom.cyclonedx.json"
        )

        multipartBodyBuilder.part("bom", sbomFile, MediaType.APPLICATION_JSON)

        val multipartBody: MultiValueMap<String, HttpEntity<*>> = multipartBodyBuilder.build()

        val httpEntity: HttpEntity<MultiValueMap<String, HttpEntity<*>>> = HttpEntity(multipartBody, headers)

        val responseEntity = restTemplate.postForEntity(
            "https://$region-vsm.leanix.net/services/vsm/discovery/v1/service", httpEntity,
            String::class.java
        )

        logger.info("Response received from VSM: $responseEntity")
        VsmSbomBoosterApplication.counter.getAndIncrement()
        if (containsComponents) {
            summaryReportService
                .appendRecord("Successfully processed repository with url: ${discoveryItem.projectUrl} \n")
        } else {
            summaryReportService
                .appendRecord(
                    "Successfully processed repository with url: " +
                        "${discoveryItem.projectUrl} with zero components\n"
                )
        }
    }
}
