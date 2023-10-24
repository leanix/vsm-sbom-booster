package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.domain.Response
import net.leanix.vsm.sbomBooster.domain.VsmSbomBoosterException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferLimitException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class ClearlyDefinedService {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ClearlyDefinedService::class.java)
    }

    fun getLicense(p: Purl): Triple<String, String, Exception?> {
        try {
            val client = WebClient.create()

            var version = "v" + p.version
            if (p.cdType in listOf("gem", "pypi", "maven", "npm")) {
                version = p.version
            }

            val url =
                "https://api.clearlydefined.io/definitions/${URLEncoder.encode(p.cdType, StandardCharsets.UTF_8)}" +
                    "/${URLEncoder.encode(p.cdProvider, StandardCharsets.UTF_8)}" +
                    "/${URLEncoder.encode(p.namespace ?: "-", StandardCharsets.UTF_8).replace("%40", "@")}" +
                    "/${URLEncoder.encode(p.packageName, StandardCharsets.UTF_8)}" +
                    "/${URLEncoder.encode(version, StandardCharsets.UTF_8)}"

            val response = client.get().uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Response::class.java)
                .block()

            if (response != null) {

                val licensed = response.licensed

                val declared = licensed?.declared ?: ""
                var discovered = ""

                val expressions = licensed?.facets?.core?.discovered?.expressions

                if (!expressions.isNullOrEmpty()) {
                    discovered = expressions[0]
                }

                return Triple(declared, discovered, null)
            }
            return Triple("", "", null)
        } catch (e: WebClientResponseException) {
            if (e.cause is DataBufferLimitException) {
                logger.warn("Skipping the license information of '${p.packageURL}' due to buffer limit exceeded.")
                return Triple("", "", null)
            } else {
                throw VsmSbomBoosterException("Error fetching license information", e)
            }
        } catch (e: Exception) {
            logger.error(e.message)
            throw VsmSbomBoosterException("Error fetching license information", e)
        }
    }
}
