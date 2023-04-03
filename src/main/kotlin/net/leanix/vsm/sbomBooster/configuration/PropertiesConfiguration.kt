package net.leanix.vsm.sbomBooster.configuration

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "leanix.vsm")
@Validated
data class PropertiesConfiguration(
    @field:NotBlank
    val mountedVolume: String,
    @field:NotBlank
    val leanIxToken: String,
    @field:NotBlank
    val githubGraphqlApiUrl: String,
    @field:NotBlank
    val githubToken: String,
    @field:NotBlank
    val githubOrganization: String,
    @field:NotBlank
    val region: String,
    val concurrencyFactor: Int,
    @field:NotBlank
    val sourceType: String,
    @field:NotBlank
    val sourceInstance: String
)
