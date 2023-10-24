package net.leanix.vsm.sbomBooster.domain

data class PackageManager(val name: String, val packages: MutableMap<String, Package>)

data class Package(val name: String, val versions: MutableMap<String, Boolean>)
