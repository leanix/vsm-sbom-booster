package net.leanix.vsm.sbomBooster.domain

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

class NoAcceptHeaderGraphQlClient(
    url: String,
    private val serializer: GraphQLClientSerializer = defaultGraphQLSerializer(),
    builder: WebClient.Builder = WebClient.builder()
) : GraphQLWebClient(url = url) {
    private val client: WebClient = builder.baseUrl(url).build()

    override suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        requestCustomizer: WebClient.RequestBodyUriSpec.() -> Unit
    ): GraphQLClientResponse<T> {
        val rawResult = client.post()
            .apply(requestCustomizer)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(serializer.serialize(request))
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
        return serializer.deserialize(rawResult, request.responseType())
    }
}
