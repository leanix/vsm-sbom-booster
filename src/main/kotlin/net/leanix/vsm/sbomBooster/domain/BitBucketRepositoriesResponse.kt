package net.leanix.vsm.sbomBooster.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class BitBucketRepositoriesResponse(
    @JsonProperty("next")
    val next: String?,
    @JsonProperty("page")
    val page: Int,
    @JsonProperty("pagelen")
    val pageLen: Int,
    @JsonProperty("size")
    val size: Int,
    @JsonProperty("values")
    val values: List<BitBucketRepository>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BitBucketRepository(
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("links")
    val links: BitBucketRepositoryLinks,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BitBucketRepositoryLinks(
    @JsonProperty("clone")
    val clone: List<BitBucketLink>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BitBucketLink(
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("href")
    val href: String
)
