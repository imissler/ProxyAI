package ee.carlrobert.codegpt.agent.strategy

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ContextOverflowDetectorTest {

    @Test
    fun `detects openai context length error`() {
        val error = IllegalStateException(
            "This model's maximum context length is 128000 tokens. (code: context_length_exceeded)"
        )

        assertThat(isContextOverflowError(error)).isTrue()
    }

    @Test
    fun `detects anthropic prompt too long error`() {
        assertThat(isContextOverflowError(RuntimeException("prompt is too long: 210000 tokens > 200000 maximum")))
            .isTrue()
    }

    @Test
    fun `detects overflow in cause chain`() {
        val error = RuntimeException("Request failed", RuntimeException("Input is too long for this model"))

        assertThat(isContextOverflowError(error)).isTrue()
    }

    @Test
    fun `ignores unrelated errors`() {
        assertThat(isContextOverflowError(RuntimeException("401 Unauthorized"))).isFalse()
        assertThat(isContextOverflowError(RuntimeException())).isFalse()
    }
}
