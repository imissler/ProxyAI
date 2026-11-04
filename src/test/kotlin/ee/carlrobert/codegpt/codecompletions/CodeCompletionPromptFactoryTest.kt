package ee.carlrobert.codegpt.codecompletions

import ai.koog.prompt.message.Message
import ee.carlrobert.codegpt.EncodingManager
import org.assertj.core.api.Assertions.assertThat
import junit.framework.TestCase

class CodeCompletionPromptFactoryTest : TestCase() {

    fun testBuildsChatFimPromptWithDeterministicCompletionParams() {
        val infillRequest = InfillRequest.Builder(
            prefix = "fun sum(a: Int, b: Int) = ",
            suffix = " + c",
            caretOffset = 0
        ).build()

        val prompt = CodeCompletionPromptFactory.createPrompt(infillRequest)
        val systemMessage = prompt.messages[0] as Message.System
        val userMessage = prompt.messages[1] as Message.User

        assertThat(prompt.params.maxTokens).isEqualTo(128)
        assertThat(prompt.params.temperature).isEqualTo(0.0)
        assertThat(systemMessage.textContent()).contains("code completion assistant")
        assertThat(userMessage.textContent()).contains("<PREFIX>")
        assertThat(userMessage.textContent()).contains("</PREFIX>")
        assertThat(userMessage.textContent()).contains("<SUFFIX>")
        assertThat(userMessage.textContent()).contains("</SUFFIX>")
        assertThat(userMessage.textContent()).contains("fun sum(a: Int, b: Int) = ")
        assertThat(userMessage.textContent()).contains(" + c")
    }

    fun testBuildTruncatesGitDiffAugmentedPrefixBackToPromptBudget() {
        val gitDiff = (1..4_000).joinToString("\n") { "diff line $it" }
        val originalPrefix = "fun main() {\n  pri"
        val infillRequest = InfillRequest.Builder(
            prefix = originalPrefix,
            suffix = "",
            caretOffset = 0
        ).gitDiff(gitDiff).build()

        assertThat(EncodingManager.getInstance().countTokens(infillRequest.prefix))
            .isLessThanOrEqualTo(MAX_PROMPT_TOKENS)
        assertThat(infillRequest.prefix).contains(originalPrefix)
        assertThat(infillRequest.prefix).doesNotContain("diff line 1")
    }
}
