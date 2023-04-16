package net.leanix.vsm.sbomBooster.domain

interface GitProviderApiService {
    fun getUsername(token: String?): String?
    fun getRepositories(token: String?, organization: String): List<Repository>
}
