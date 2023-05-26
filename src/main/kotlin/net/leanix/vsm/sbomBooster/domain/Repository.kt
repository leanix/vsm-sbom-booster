package net.leanix.vsm.sbomBooster.domain

data class Repository(
    val cloneUrl: String,
    val sourceType: String,
    val sourceInstance: String,
    val name: String,
    val repoId: String
)
