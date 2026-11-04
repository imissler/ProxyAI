package ee.carlrobert.codegpt.agent.strategy

private val CONTEXT_OVERFLOW_MARKERS = listOf(
    "context_length_exceeded",
    "context length",
    "context window",
    "maximum context",
    "prompt is too long",
    "prompt too long",
    "input is too long",
    "input too long",
    "too many tokens",
    "reduce the length",
    "exceed context",
    "exceeds the limit of",
    "input length and `max_tokens` exceed",
)

/**
 * Best-effort detection of provider errors that say the prompt did not fit into the model's
 * context window. Providers do not share an error format, so the message chain is matched.
 */
fun isContextOverflowError(throwable: Throwable): Boolean {
    var current: Throwable? = throwable
    var depth = 0
    while (current != null && depth < 10) {
        val message = current.message?.lowercase()
        if (message != null && CONTEXT_OVERFLOW_MARKERS.any { message.contains(it) }) {
            return true
        }
        current = current.cause?.takeIf { it !== current }
        depth++
    }
    return false
}
