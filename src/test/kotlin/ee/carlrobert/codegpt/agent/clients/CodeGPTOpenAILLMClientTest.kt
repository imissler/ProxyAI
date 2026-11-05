package ee.carlrobert.codegpt.agent.clients

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIResponsesParams
import ai.koog.prompt.executor.clients.openai.base.models.ReasoningEffort
import ai.koog.prompt.executor.clients.openai.models.ReasoningConfig
import ai.koog.prompt.executor.clients.openai.models.ReasoningSummary
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class CodeGPTOpenAILLMClientTest {

    private lateinit var server: HttpServer
    private val requests = CopyOnWriteArrayList<JsonObject>()

    // No Thinking capability, like the catalogue's OpenAI models.
    private val model = LLModel(
        provider = LLMProvider.OpenAI,
        id = "gpt-5.4",
        capabilities = listOf(LLMCapability.Completion, LLMCapability.OpenAIEndpoint.Responses),
        contextLength = 1_050_000,
        maxOutputTokens = 128_000,
    )

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/responses") { exchange ->
            requests += Json.parseToJsonElement(
                exchange.requestBody.readAllBytes().decodeToString()
            ).jsonObject
            val bytes = RESPONSE.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun configuredEffortIsSentEvenWithoutThinkingCapability() = runBlocking<Unit> {
        createClient(effort = "xhigh").execute(reasoningPrompt(), model, emptyList())

        assertThat(requests.single()["reasoning"].toString())
            .isEqualTo("""{"effort":"xhigh","summary":"auto"}""")
    }

    @Test
    fun noConfiguredEffortLeavesParamsUntouched() = runBlocking<Unit> {
        createClient(effort = null).execute(reasoningPrompt(), model, emptyList())

        assertThat(requests.single()).doesNotContainKey("reasoning")
    }

    private val baseClient = HttpClient(Apache5)

    private fun createClient(effort: String?) = CodeGPTOpenAILLMClient(
        apiKey = "test",
        organization = null,
        baseUrl = "http://127.0.0.1:${server.address.port}",
        model = "gpt-5.4",
        reasoningEffort = { effort },
        baseClient = baseClient,
        httpClientFactory = KtorKoogHttpClient.Factory(baseClient = baseClient),
    )

    private fun reasoningPrompt() = prompt(
        "openai-effort",
        OpenAIResponsesParams(
            reasoning = ReasoningConfig(
                effort = ReasoningEffort.MEDIUM,
                summary = ReasoningSummary.AUTO
            )
        )
    ) { user("hi") }

    companion object {
        private const val RESPONSE = """{"id":"r","object":"response","created_at":1,"model":"gpt-5.4","output":[{"type":"message","id":"m","role":"assistant","status":"completed","content":[{"type":"output_text","text":"ok","annotations":[]}]}],"parallel_tool_calls":true,"status":"completed","text":{},"usage":{"input_tokens":1,"input_tokens_details":{"cached_tokens":0},"output_tokens":1,"output_tokens_details":{"reasoning_tokens":0},"total_tokens":2}}"""
    }
}
