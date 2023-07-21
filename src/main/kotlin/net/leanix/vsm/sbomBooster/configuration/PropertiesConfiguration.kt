package net.leanix.vsm.sbomBooster.configuration

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "leanix.vsm")
@Validated
data class PropertiesConfiguration(
    // Runtime settings
    @field:NotBlank
    val mountedVolume: String,
    @field:NotNull
    val concurrencyFactor: Int,
    @field:NotNull
    val analysisTimeout: Long,

    // LeanIX configs
    @field:NotBlank
    val leanIxToken: String,
    @field:NotBlank
    var leanIxHost: String,

    // Discovery API data
    @field:NotBlank
    val sourceType: String,
    var sourceInstance: String,

    // GIT
    @field:NotBlank
    val gitProvider: String,

    // GITHUB
    @field:NotBlank
    val githubApiHost: String,
    val githubToken: String,
    val githubOrganization: String,

    // GITLAB
    @field:NotBlank
    val gitlabApiHost: String,
    val gitlabToken: String,
    val gitlabGroup: String,

    // BITBUCKET
    val bitbucketKey: String,
    val bitbucketSecret: String,
    val bitbucketWorkspace: String
) {
    val githubGraphqlApiUrl: String
        get() {
            if (githubApiHost == "api.github.com") {
                return "https://$githubApiHost/graphql"
            }
            return "https://$githubApiHost/api/graphql"
        }

    val gitlabGraphqlApiUrl: String
        get() = "https://$gitlabApiHost/api/graphql"
}
