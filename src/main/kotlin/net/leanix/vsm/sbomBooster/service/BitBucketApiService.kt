package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.BitBucketAuthResponse
import net.leanix.vsm.sbomBooster.domain.BitBucketRepositoriesResponse
import net.leanix.vsm.sbomBooster.domain.GitProviderApiService
import net.leanix.vsm.sbomBooster.domain.Repository
import org.slf4j.Logger
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
        val logger: Logger = LoggerFactory.getLogger(BitBucketApiService::class.java)
    }

    override fun getUsername(token: String?): String? {
        return propertiesConfiguration.bitbucketWorkspace
    }

    override fun getRepositories(token: String?, organization: String): List<Repository> {
        val bearerToken = authenticate(token)

        return getPaginatedRepositories(bearerToken, organization, null)
    }

    fun getPaginatedRepositories(bearerToken: String?, organization: String, pageUrl: String?): List<Repository> {

        val restTemplate = RestTemplate()
        val headers = HttpHeaders()

        headers.set("Authorization", "Bearer $bearerToken")
        val httpEntity: HttpEntity<*> = HttpEntity<MultiValueMap<String, String>>(headers)

        val url: String = if (pageUrl !== null)
            pageUrl
        else
            "https://api.bitbucket.org/2.0/repositories/$organization"

        val responseEntity = restTemplate.exchange(
            url, HttpMethod.GET, httpEntity,
            BitBucketRepositoriesResponse::class.java
        )

        val bbRepositoriesResponse = responseEntity.body!!.values

        val repositories = mutableListOf<Repository>()
        for (bbRepo in bbRepositoriesResponse) {
            // Finds the http clone URL and removes the username from it
            val cloneUrl =
                bbRepo.links.clone.firstOrNull { it.name == "https" }?.href?.replaceFirst("[^/]+@".toRegex(), "")

            // Figure out what sourceInstance to use
            val sourceInstance: String = if (propertiesConfiguration.sourceInstance == "")
                propertiesConfiguration.bitbucketWorkspace
            else
                propertiesConfiguration.sourceInstance

            repositories.add(
                Repository(
                    cloneUrl ?: "",
                    propertiesConfiguration.sourceType,
                    sourceInstance,
                    bbRepo.name
                )
            )
        }

        if (responseEntity.body!!.next !== null && responseEntity.body!!.next !== "") {
            val bbRepos = getPaginatedRepositories(bearerToken, organization, responseEntity.body!!.next)
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

        val httpEntity: HttpEntity<*> = HttpEntity<MultiValueMap<String, String>>(requestBody, headers)

        val responseEntity = restTemplate.postForEntity(
            "https://bitbucket.org/site/oauth2/access_token", httpEntity,
            BitBucketAuthResponse::class.java
        )

        return responseEntity.body!!.accessToken
    }
}
