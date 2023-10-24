package net.leanix.vsm.sbomBooster.service

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import kotlinx.coroutines.runBlocking
import net.leanix.vsm.sbomBooster.configuration.PropertiesConfiguration
import net.leanix.vsm.sbomBooster.domain.GitProviderApiService
import net.leanix.vsm.sbomBooster.domain.NoAcceptHeaderGraphQlClient
import net.leanix.vsm.sbomBooster.domain.Package
import net.leanix.vsm.sbomBooster.domain.PackageManager
import net.leanix.vsm.sbomBooster.domain.Repository
import net.leanix.vsm.sbomBooster.graphql.generated.github.GetDependenciesPaginated
import net.leanix.vsm.sbomBooster.graphql.generated.github.GetRepositoriesPaginated
import net.leanix.vsm.sbomBooster.graphql.generated.github.GetUsername
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

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

    fun getAllDependencies(token: String?, repoOwner: String, repoName: String): MutableMap<String, PackageManager> {
        val dependencies: MutableMap<String, PackageManager> = mutableMapOf()

        var manifestCursor: String? = null

        while (true) {
            logger.debug("Getting Dependency Manifest for '$repoOwner/$repoName'")

            val nextManifestCursor = getDependenciesFromManifest(
                token, repoOwner, repoName, manifestCursor, dependencies
            )

            if (nextManifestCursor == null) {
                break
            } else {
                manifestCursor = nextManifestCursor
            }
        }

        return dependencies
    }

    @Suppress("ComplexMethod", "LongMethod")
    fun getDependenciesFromManifest(
        token: String?,
        repoOwner: String,
        repoName: String,
        manifestCursor: String?,
        dependencies: MutableMap<String, PackageManager>
    ): String? {
        logger.debug("Getting page of dependencies")

        val builder = WebClient.builder().defaultHeader("Accept", "application/vnd.github.hawkgirl-preview+json")

        val client = NoAcceptHeaderGraphQlClient(url = propertiesConfiguration.githubGraphqlApiUrl, builder = builder)
        var hasNextManifestPage: Boolean? = true
        var hasNextDependencyPage: Boolean? = true
        var nextManifestCursor: String? = null
        var dependencyCursor: String? = null

        do {
            runBlocking {
                val repositoryDependencies =
                    GetDependenciesPaginated(
                        GetDependenciesPaginated.Variables(
                            repoName,
                            repoOwner,
                            50,
                            manifestCursor,
                            dependencyCursor
                        )
                    )

                try {
                    val result = client.execute(repositoryDependencies) {
                        header("Authorization", "Bearer $token")
                    }
                    if (result.errors?.isNotEmpty() == true) {
                        for (error in result.errors!!) {
                            logger.error("$repoOwner/$repoName Result: ${error.message}")
                        }
                        hasNextDependencyPage = false
                    } else {

                        val manifestPage =
                            result.data?.repository?.dependencyGraphManifests?.nodes?.get(0)

                        if (manifestPage != null) {

                            hasNextManifestPage =
                                result.data?.repository?.dependencyGraphManifests?.pageInfo?.hasNextPage
                            hasNextDependencyPage =
                                manifestPage.dependencies?.pageInfo?.hasNextPage

                            nextManifestCursor =
                                result.data?.repository?.dependencyGraphManifests?.pageInfo?.endCursor
                            dependencyCursor =
                                manifestPage.dependencies?.pageInfo?.endCursor

                            manifestPage.dependencies?.nodes?.forEach {

                                val packageManager = it?.packageManager?.lowercase()
                                val packageName = it?.packageName?.lowercase()
                                var packageVersion = ""

                                if (it?.requirements != null && it.requirements.length > 2) {
                                    packageVersion = it.requirements.substring(2)
                                }

                                if (packageVersion.startsWith("v")) {
                                    packageVersion = packageVersion.substring(1)
                                }

                                if (packageManager != null && packageName != null) {
                                    if (dependencies.containsKey(packageManager)) {
                                        if (dependencies[packageManager]!!.packages.containsKey(packageName)) {
                                            dependencies[packageManager]!!.packages[packageName]!!
                                                .versions[packageVersion] = true
                                        } else {
                                            dependencies[packageManager]!!.packages[packageName] =
                                                Package(
                                                    packageName, mutableMapOf(Pair(packageVersion, true))
                                                )
                                        }
                                    } else {
                                        dependencies[packageManager] = PackageManager(
                                            packageManager,
                                            mutableMapOf(
                                                Pair(
                                                    packageName,
                                                    Package(
                                                        packageName, mutableMapOf(Pair(packageVersion, true))
                                                    )
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            logger.error("I think this null check is useless")
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e.message)
                    hasNextDependencyPage = false
                }
            }
        }
        while (hasNextDependencyPage == true)

        return if (hasNextManifestPage == true) nextManifestCursor else null
    }
}
