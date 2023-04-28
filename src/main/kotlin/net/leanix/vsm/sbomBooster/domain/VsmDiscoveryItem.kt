package net.leanix.vsm.sbomBooster.domain

data class VsmDiscoveryItem(
    val projectUrl: String,
    val downloadedFolder: String,
    val sourceType: String,
    val sourceInstance: String,
    val name: String
)
