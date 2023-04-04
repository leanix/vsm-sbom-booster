package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.VsmSbomBoosterApplication
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
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
    private val propertiesConfiguration: PropertiesConfiguration
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VsmDiscoveryService::class.java)
    }

    fun sendToVsm(projectUrl: String, downloadedFolder: String, leanIxToken: String, region: String) {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.set("Authorization", "Bearer $leanIxToken")

        val multipartBodyBuilder = MultipartBodyBuilder()

        multipartBodyBuilder.part("id", projectUrl.substringAfterLast("/"))
        multipartBodyBuilder.part("sourceType", propertiesConfiguration.sourceType)
        multipartBodyBuilder.part("sourceInstance", propertiesConfiguration.sourceInstance)
        multipartBodyBuilder.part("name", projectUrl.substringAfterLast("/"))

        val sbomFile: Resource = FileSystemResource(
            "${ Paths.get("tempDir").toAbsolutePath()}" +
                "/$downloadedFolder/bom.cyclonedx.json"
        )

        val sbomByteArray = Files.readAllBytes(Paths.get("tempDir", downloadedFolder, "bom.cyclonedx.json"))

        val parser = BomParserFactory.createParser(sbomByteArray)

        val bom = parser.parse(sbomByteArray)

        if (!bom.components.isNullOrEmpty()) {
            multipartBodyBuilder.part("bom", sbomFile, MediaType.APPLICATION_JSON)

            val multipartBody: MultiValueMap<String, HttpEntity<*>> = multipartBodyBuilder.build()

            val httpEntity: HttpEntity<MultiValueMap<String, HttpEntity<*>>> = HttpEntity(multipartBody, headers)

            val responseEntity = restTemplate.postForEntity(
                "https://$region-vsm.leanix.net/services/vsm/discovery/v1/service", httpEntity,
                String::class.java
            )

            logger.info("Response received from VSM: $responseEntity")
            VsmSbomBoosterApplication.counter.getAndIncrement()
        } else {
            logger.info("No components found in the SBOM file for repository $projectUrl")
        }
    }
}
