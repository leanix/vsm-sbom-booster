package net.leanix.vsm.sbomBooster.service

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import kotlinx.coroutines.runBlocking
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.GitProviderApiService
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.graphql.generated.github.GetRepositoriesPaginated
import net.leanix.vsm.sbomBooster.graphql.generated.github.GetUsername
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitHubApiService(
    private val propertiesConfiguration: PropertiesConfiguration
) : GitProviderApiService {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(GitHubApiService::class.java)
    }

    override fun getUsername(token: String?): String? {
        val client = GraphQLWebClient(url = propertiesConfiguration.githubGraphqlApiUrl)
        var username: String? = null
        runBlocking {
            val getUsernameQuery = GetUsername()
            @Suppress("TooGenericExceptionCaught")
            try {
                val result = client.execute(getUsernameQuery) {
                    header("Authorization", "Bearer $token")
                }
                if (result.errors?.isNotEmpty() == true) {
                    logger.error("Result: ${result.errors?.get(0)?.message}")
                } else {
                    username = result.data?.viewer?.login
                }
            } catch (e: Exception) {
                logger.error("${e.message}")
            }
        }

        return username
    }

    override fun getRepositories(token: String?, organization: String): List<Repository> {
        val client = GraphQLWebClient(url = propertiesConfiguration.githubGraphqlApiUrl)
        var resultscounter: Int? = 0
        var afterParameter: String? = null
        val repositoriesList = mutableListOf<Repository>()

        do {
            runBlocking {
                val getRepositoriesPaginated =
                    GetRepositoriesPaginated(
                        GetRepositoriesPaginated.Variables(
                            organization,
                            50,
                            afterParameter
                        )
                    )

                try {
                    val result = client.execute(getRepositoriesPaginated) {
                        header("Authorization", "Bearer $token")
                    }
                    if (result.errors?.isNotEmpty() == true) {
                        logger.error("Result: ${result.errors?.get(0)?.message}")
                    } else {
                        resultscounter = result.data?.viewer?.organization?.repositories?.edges?.size
                        if (resultscounter != 0) {
                            afterParameter = result.data?.viewer?.organization?.repositories?.edges?.last()?.cursor
                            result.data?.viewer?.organization?.repositories?.edges?.forEach {
                                // Figure out what sourceInstance to use
                                val sourceInstance: String = if (propertiesConfiguration.sourceInstance == "")
                                    propertiesConfiguration.githubOrganization
                                else
                                    propertiesConfiguration.sourceInstance

                                repositoriesList.add(
                                    Repository(
                                        it?.node?.url.toString(),
                                        propertiesConfiguration.sourceType,
                                        sourceInstance,
                                        it?.node?.name.toString(),
                                        it?.node?.id.toString()
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e.message)
                }
            }
        }
        while (resultscounter != 0)

        return repositoriesList
    }
}
