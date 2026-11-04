package ee.carlrobert.codegpt.agent.strategy

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CompactionSplitTest {

    private fun system() = Message.System("sys", RequestMetaInfo.Empty)
    private fun user(text: String) = Message.User(text, RequestMetaInfo.Empty)
    private fun assistant(text: String) = Message.Assistant(text, ResponseMetaInfo.Empty)
    private fun call(id: String) = Message.Assistant(
        MessagePart.Tool.Call(id = id, tool = "Read", args = "{}"),
        ResponseMetaInfo.Empty
    )

    private fun result(id: String) = Message.User(
        MessagePart.Tool.Result(id = id, tool = "Read", output = "out"),
        RequestMetaInfo.Empty
    )

    @Test
    fun keepsLastMessagesAndSummarizesTheRest() {
        val messages = listOf(
            system(), user("goal"), assistant("a1"), user("u2"), assistant("a2"), user("u3"),
            assistant("a3")
        )

        val (head, tail) = splitForCompaction(messages, 2)

        assertThat(head).isEqualTo(messages.subList(0, 5))
        assertThat(tail).isEqualTo(messages.subList(5, 7))
    }

    @Test
    fun neverSeparatesToolResultFromItsCall() {
        val messages = listOf(
            system(), user("goal"), assistant("a1"), call("1"), result("1"), call("2"),
            result("2")
        )

        val (head, tail) = splitForCompaction(messages, 1)

        assertThat(tail.first()).isEqualTo(messages[5])
        assertThat(head).isEqualTo(messages.subList(0, 5))
    }

    @Test
    fun movesBackOverConsecutiveToolResults() {
        val messages = listOf(
            system(), user("goal"), assistant("a1"), call("1"), result("1"), result("1b")
        )

        val (head, tail) = splitForCompaction(messages, 1)

        assertThat(tail).isEqualTo(messages.subList(3, 6))
        assertThat(head).isEqualTo(messages.subList(0, 3))
    }

    @Test
    fun returnsEverythingAsHeadWhenNothingOlderToSummarize() {
        val messages = listOf(system(), user("goal"), assistant("a1"), user("u2"))

        val (head, tail) = splitForCompaction(messages, 4)

        assertThat(head).isEqualTo(messages)
        assertThat(tail).isEmpty()
    }
}
