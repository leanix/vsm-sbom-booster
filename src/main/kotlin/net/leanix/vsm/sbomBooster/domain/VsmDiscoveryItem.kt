package net.leanix.vsm.sbomBooster.domain

data class VsmDiscoveryItem(
    val projectUrl: String,
    val ortFolder: String,
    val sourceType: String,
    val sourceInstance: String,
    val name: String,
    val repoId: String
)
