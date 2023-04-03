package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.domain.MtMResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class MtMService {

    fun getAccessToken(region: String, lxToken: String): String? {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()

        val auth: String = "apitoken:$lxToken"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        headers.set("Authorization", "Basic $encodedAuth")

        val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
        requestBody.add("grant_type", "client_credentials")

        val httpEntity: HttpEntity<*> = HttpEntity<MultiValueMap<String, String>>(requestBody, headers)

        val responseEntity = restTemplate.postForEntity(
            "https://$region.leanix.net/services/mtm/v1/oauth2/token ", httpEntity,
            MtMResponse::class.java
        )

        return responseEntity.body?.accessToken
    }
}
