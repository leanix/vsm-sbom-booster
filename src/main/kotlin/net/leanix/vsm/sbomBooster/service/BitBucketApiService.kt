package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.BitBucketAuthResponse
import net.leanix.vsm.sbomBooster.domain.BitBucketRepositoriesResponse
import net.leanix.vsm.sbomBooster.domain.GitProviderApiService
import net.leanix.vsm.sbomBooster.domain.Repository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class BitBucketApiService(
    private val propertiesConfiguration: PropertiesConfiguration
) : GitProviderApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(BitBucketApiService::class.java)
    }

    override fun getUsername(token: String?): String? {
        return propertiesConfiguration.bitbucketWorkspace
    }

    override fun getRepositories(token: String?, organization: String): List<Repository> {
        val bearerToken = authenticate(token)
        logger.info("Bearer token: $bearerToken")

        return getPaginatedRepositories(bearerToken, organization, null)
    }

    private fun getPaginatedRepositories(
        bearerToken: String?,
        organization: String,
        pageUrl: String?
    ): List<Repository> {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()

        headers.set("Authorization", "Bearer $bearerToken")
        val httpEntity: HttpEntity<MultiValueMap<String, String>> =
            HttpEntity(headers)

        val url = pageUrl ?: "https://api.bitbucket.org/2.0/repositories/$organization"
        logger.info("API URL: $url")

        val responseEntity = restTemplate.exchange(
            url, HttpMethod.GET, httpEntity,
            BitBucketRepositoriesResponse::class.java
        )
        val bbRepositoriesResponse = responseEntity.body?.values ?: emptyList()

        val repositories = mutableListOf<Repository>()
        for (bbRepo in bbRepositoriesResponse) {
            val cloneUrl =
                bbRepo.links.clone.firstOrNull { it.name == "https" }?.href?.replaceFirst("[^/]+@".toRegex(), "")
            val sourceInstance = if (propertiesConfiguration.sourceInstance.isBlank())
                propertiesConfiguration.bitbucketWorkspace
            else
                propertiesConfiguration.sourceInstance

            repositories.add(
                Repository(
                    cloneUrl.orEmpty(),
                    propertiesConfiguration.sourceType,
                    sourceInstance,
                    bbRepo.name
                )
            )
        }

        if (responseEntity.body?.next.isNullOrBlank().not()) {
            val bbRepos = getPaginatedRepositories(bearerToken, organization, responseEntity.body?.next)
            repositories.addAll(bbRepos)
        }

        return repositories.toList()
    }

    fun authenticate(token: String?): String? {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()

        val encodedAuth = Base64.getEncoder().encodeToString(token?.toByteArray())
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        headers.set("Authorization", "Basic $encodedAuth")

        val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
        requestBody.add("grant_type", "client_credentials")

        val httpEntity: HttpEntity<MultiValueMap<String, String>> =
            HttpEntity(requestBody, headers)

        val responseEntity = restTemplate.postForEntity(
            "https://bitbucket.org/site/oauth2/access_token", httpEntity,
            BitBucketAuthResponse::class.java
        )

        val accessToken = responseEntity.body?.accessToken
        logger.info("Access token: $accessToken")

        return accessToken
    }
}
