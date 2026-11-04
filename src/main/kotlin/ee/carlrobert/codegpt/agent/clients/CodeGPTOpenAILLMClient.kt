package ee.carlrobert.codegpt.agent.clients

import ai.koog.http.client.KoogHttpClient
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIResponsesParams
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.agents.core.tools.ToolDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ee.carlrobert.codegpt.codecompletions.InfillRequest
import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders

class CodeGPTOpenAILLMClient(
    private val apiKey: String,
    private val organization: String?,
    private val baseUrl: String,
    private val model: String,
    private val reasoningEffort: () -> String? = { null },
    private val baseClient: HttpClient = HttpClientProvider.createHttpClient(),
    httpClientFactory: KoogHttpClient.Factory = HttpClientProvider.resolveKoogHttpClientFactory(baseClient)
) : OpenAILLMClient(
    apiKey = apiKey,
    settings = OpenAIClientSettings(baseUrl = baseUrl),
    httpClientFactory = httpClientFactory
),
    CodeCompletionCapable {

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Message.Assistant {
        return super.execute(withConfiguredReasoning(prompt), model, tools)
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> {
        return super.executeStreaming(withConfiguredReasoning(prompt), model, tools)
    }

    /**
     * Applies the effort chosen in the settings. It is sent through additionalProperties because
     * Koog drops `reasoning` for models without the Thinking capability and its effort enum has no
     * "xhigh".
     */
    private fun withConfiguredReasoning(prompt: Prompt): Prompt {
        val effort = reasoningEffort()?.takeIf { it.isNotBlank() } ?: return prompt
        val params = prompt.params as? OpenAIResponsesParams ?: return prompt
        return prompt.withParams(
            params.copy(
                reasoning = null,
                additionalProperties = (params.additionalProperties ?: emptyMap()) +
                        ("reasoning" to buildJsonObject {
                            put("effort", effort)
                            put("summary", "auto")
                        })
            )
        )
    }

    override suspend fun getCodeCompletion(infillRequest: InfillRequest): String {
        val headers = buildMap {
            if (apiKey.isNotBlank()) {
                put(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            put("X-LLM-Application-Tag", "codegpt")
            organization?.takeIf { it.isNotBlank() }?.let {
                put("OpenAI-Organization", it)
            }
        }

        return parseOpenAITextCompletion(
            postCompletionJson(
                client = baseClient,
                url = "$baseUrl/v1/completions",
                headers = headers,
                body = buildOpenAIStyleCompletionBody(model, infillRequest)
            )
        )
    }
}
