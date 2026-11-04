package ee.carlrobert.codegpt.agent.strategy

import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ee.carlrobert.codegpt.agent.normalizeToolArgumentsJson
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class SingleRunStrategyProviderTest {

    @Test
    fun `extract assistant output combines reasoning and assistant text`() {
        val response = Message.Assistant(
            listOf(
                MessagePart.Reasoning("Reasoning text"),
                MessagePart.Text("Final answer")
            ),
            ResponseMetaInfo.Empty
        )

        val actual = extractAssistantOutput(response)

        assertThat(actual).isEqualTo("<think>Reasoning text</think>\nFinal answer")
    }

    @Test
    fun `extract assistant output returns assistant text when there is no reasoning`() {
        val response = Message.Assistant("Final answer", ResponseMetaInfo.Empty)

        assertThat(extractAssistantOutput(response)).isEqualTo("Final answer")
    }

    @Test
    fun `extract assistant output falls back to reasoning when assistant text is blank`() {
        val response = Message.Assistant(
            listOf(
                MessagePart.Reasoning("Reasoning text"),
                MessagePart.Text("")
            ),
            ResponseMetaInfo.Empty
        )

        val actual = extractAssistantOutput(response)

        assertThat(actual).isEqualTo("<think>Reasoning text</think>")
    }

    @Test
    fun `appendable responses keeps responses untouched for every provider`() {
        val response = Message.Assistant(
            listOf(
                MessagePart.Reasoning("Reasoning text"),
                MessagePart.Text("I will inspect the file."),
                MessagePart.Tool.Call(
                    id = "tool-1",
                    tool = "Read",
                    args = """{"path":"build.gradle.kts"}"""
                )
            ),
            ResponseMetaInfo.Empty
        )

        listOf(LLMProvider.OpenAI, LLMProvider.Google).forEach { provider ->
            val actual = appendableResponses(listOf(response), provider)

            assertThat(actual).containsExactly(response)
            val toolCall = actual.single().parts.filterIsInstance<MessagePart.Tool.Call>().single()
            assertThat(toolCall.tool).isEqualTo("Read")
            assertThat(toolCall.args).isEqualTo("""{"path":"build.gradle.kts"}""")
        }
    }

    @Test
    fun `normalize tool arguments unwraps string encoded json object`() {
        val actual = normalizeToolArgumentsJson("\"{\\\"file_path\\\":\\\"/tmp/fixture.txt\\\"}\"")

        assertThat(actual).isEqualTo("""{"file_path":"/tmp/fixture.txt"}""")
    }
}
