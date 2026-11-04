package ee.carlrobert.codegpt.agent

import ee.carlrobert.codegpt.ui.textarea.header.tag.TagDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
class MessageWithContext(
    val text: String,
    @Transient
    val tags: List<TagDetails> = emptyList(),
    val uiVisible: Boolean = true,
    val uiText: String = text,
    /** Only compacts the session history; nothing is sent to the model. */
    val compactOnly: Boolean = false
) {

    @Transient
    val id: UUID = UUID.randomUUID()
}
