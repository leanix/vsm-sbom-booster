package net.leanix.vsm.sbomBooster.domain

data class Response(
    val described: Described?,
    val licensed: Licensed?,
    val files: List<Files>?,
    val coordinates: Coordinates?,
    @Suppress("ConstructorParameterNaming")
    val _meta: _meta?,
    val scores: Scores?
)

data class Described(
    val releaseDate: String?,
    val sourceLocation: SourceLocation?,
    val urls: Urls?,
    val projectWebsite: String?,
    val issueTracker: String?,
    val hashes: Hashes?,
    val files: Int?,
    val tools: List<String>?,
    val toolScore: ToolScore?,
    val score: Score?
)

data class Files(
    val path: String?,
    val hashes: Hashes?
)

data class Coordinates(
    val type: String?,
    val provider: String?,
    val namespace: String?,
    val name: String?,
    val revision: String?
)

data class Scores(
    val effective: Int?,
    val tool: Int?
)

@Suppress("ClassNaming")
data class _meta(
    val schemaVersion: String?,
    val updated: String?
)

data class Licensed(
    val declared: String?,
    val toolScore: ToolScore?,
    val facets: Facets?,
    val score: Score?
)

data class Facets(
    val core: Core?
)

data class Core(
    val attribution: Attribution?,
    val discovered: Discovered?,
    val files: Int?
)

data class Attribution(
    val unknown: Int?,
    val parties: List<String>?
)

data class Discovered(
    val unknown: Int?,
    val expressions: List<String>?
)

data class SourceLocation(
    val type: String?,
    val provider: String?,
    val namespace: String?,
    val name: String?,
    val revision: String?,
    val url: String?
)

data class ToolScore(
    val total: Int?,
    val declared: Int?,
    val discovered: Int?,
    val consistency: Int?,
    val spdx: Int?,
    val texts: Int?
)

data class Score(
    val total: Int?,
    val declared: Int?,
    val discovered: Int?,
    val consistency: Int?,
    val spdx: Int?,
    val texts: Int?
)

data class Hashes(
    val sha1: String?,
    val sha256: String?
)

data class Urls(
    val registry: String?,
    val version: String?,
    val download: String?
)
