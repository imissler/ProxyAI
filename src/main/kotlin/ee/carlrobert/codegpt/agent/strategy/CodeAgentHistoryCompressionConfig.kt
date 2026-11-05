package ee.carlrobert.codegpt.agent.strategy

import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.prompt.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.tokenizer.PromptTokenizer
import ee.carlrobert.codegpt.settings.configuration.AutoCompactSettings

/**
 * Compaction starts once the history uses [thresholdPercent] of the available input budget,
 * which leaves headroom for the next turn and for the compression requests themselves.
 */
fun autoCompactThreshold(availableInputTokens: Long, thresholdPercent: Int): Long =
    (availableInputTokens * AutoCompactSettings.normalizeThresholdPercent(thresholdPercent) / 100)
        .coerceAtLeast(1L)

/**
 * Reads the auto-compact settings on every call, so that changes apply to running sessions.
 */
fun buildAutoCompactPredicate(availableInputTokens: Long): (Prompt, PromptTokenizer) -> Boolean =
    { prompt, tokenizer ->
        AutoCompactSettings.isEnabled() &&
                buildHistoryTooBigPredicate(
                    autoCompactThreshold(availableInputTokens, AutoCompactSettings.thresholdPercent())
                )(prompt, tokenizer)
    }

fun buildHistoryTooBigPredicate(maxPromptTokens: Long): (Prompt, PromptTokenizer) -> Boolean =
    { prompt, tokenizer ->
        val tokenCount = if (prompt.latestTokenUsage == 0) {
            tokenizer.tokenCountFor(prompt)
        } else {
            prompt.latestTokenUsage
        }
        tokenCount.toLong() >= maxPromptTokens
    }

/** Number of most recent messages that stay verbatim when the history is compacted. */
const val COMPACT_KEEP_LAST_MESSAGES = 4

/**
 * Splits [messages] into the part to summarise and the recent tail that stays verbatim.
 *
 * The tail holds at least [keepLast] messages. Its start moves back so that a tool result is never
 * separated from the tool call that produced it. The head is empty when there is nothing besides
 * system messages and the first user message to summarise.
 */
internal fun splitForCompaction(
    messages: List<Message>,
    keepLast: Int
): Pair<List<Message>, List<Message>> {
    var tailStart = (messages.size - keepLast).coerceAtLeast(0)
    while (tailStart > 0 && messages[tailStart].isToolResultMessage()) {
        tailStart--
    }
    val head = messages.subList(0, tailStart)
    val summarizable = head.count { it !is Message.System }
    // The first user message is always kept by Koog, so a single one is nothing to summarise.
    if (summarizable <= 1) {
        return messages to emptyList()
    }
    return head to messages.subList(tailStart, messages.size)
}

private fun Message.isToolResultMessage(): Boolean =
    this is Message.User && parts.any { it is MessagePart.Tool.Result }

/**
 * Summarises the older history in a single model request and keeps the latest messages verbatim.
 *
 * Replaces the fact-retrieval strategy, which sent one request per concept with the whole
 * history attached. That is very slow on local models such as llama.cpp, where every request
 * has to process the full prompt again.
 */
object SummarizeOlderHistoryStrategy : HistoryCompressionStrategy() {
    override suspend fun compress(
        llmSession: AIAgentLLMWriteSession,
        memoryMessages: List<Message>
    ) {
        val original = llmSession.prompt.messages
        val (head, tail) = splitForCompaction(original, COMPACT_KEEP_LAST_MESSAGES)

        llmSession.prompt = llmSession.prompt.withMessages { head }
        val tldr = compressPromptIntoTLDR(llmSession)
        val compressed = composeMessageHistory(head, tldr, memoryMessages) + tail
        llmSession.prompt = llmSession.prompt.withMessages { compressed }
    }
}

val CODE_AGENT_COMPRESSION: HistoryCompressionStrategy = SummarizeOlderHistoryStrategy
