package net.leanix.vsm.sbomBooster.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MtMResponse(
    @JsonProperty("access_token")
    val accessToken: String,

)
