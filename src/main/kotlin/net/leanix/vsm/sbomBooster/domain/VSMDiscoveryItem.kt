package net.leanix.vsm.sbomBooster.domain

data class VSMDiscoveryItem(
    val projectUrl: String,
    val downloadedFolder: String,
    val sourceType: String,
    val sourceInstance: String,
    val name: String
)
