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

    // LeanIX configs
    @field:NotBlank
    val leanIxToken: String,
    @field:NotBlank
    val leanIxRegion: String,
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
    val gitlabGroup: String
) {
    val githubGraphqlApiUrl: String
        get() = "https://$githubApiHost/graphql"

    val gitlabGraphqlApiUrl: String
        get() = "https://$gitlabApiHost/api/graphql"
}
