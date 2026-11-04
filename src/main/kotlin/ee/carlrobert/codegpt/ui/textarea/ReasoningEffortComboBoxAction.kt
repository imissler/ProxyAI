package ee.carlrobert.codegpt.ui.textarea

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import ee.carlrobert.codegpt.CodeGPTBundle
import ee.carlrobert.codegpt.agent.clients.CodeGPTAnthropicLLMClient
import ee.carlrobert.codegpt.settings.models.ModelSettings
import ee.carlrobert.codegpt.settings.service.FeatureType
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.anthropic.AnthropicSettings
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings
import ai.koog.prompt.llm.LLMCapability
import ee.carlrobert.codegpt.toolwindow.ui.ModelListPopups
import java.awt.Color
import javax.swing.JComponent

/**
 * Chooses the reasoning effort that Claude 5.5 and OpenAI reasoning models use for [featureType].
 */
class ReasoningEffortComboBoxAction(
    private val featureType: FeatureType,
) : ComboBoxAction() {

    companion object {
        private val ANTHROPIC_EFFORTS = listOf("low", "medium", "high", "xhigh", "max")
        private val OPENAI_EFFORTS = listOf("low", "medium", "high", "xhigh")

        fun isApplicable(featureType: FeatureType): Boolean {
            val selection = runCatching {
                ModelSettings.getInstance().getModelSelectionForFeature(featureType)
            }.getOrNull() ?: return false
            return when (selection.provider) {
                ServiceType.ANTHROPIC ->
                    CodeGPTAnthropicLLMClient.usesAdaptiveThinking(selection.llmModel)

                ServiceType.OPENAI ->
                    selection.llmModel.supports(LLMCapability.OpenAIEndpoint.Responses)

                else -> false
            }
        }
    }

    private var livePresentation: Presentation? = null

    init {
        isSmallVariant = true
        refreshPresentation(templatePresentation)
    }

    fun createCustomComponent(place: String): JComponent {
        return createCustomComponent(templatePresentation, place)
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String
    ): JComponent {
        val button = createComboBoxButton(presentation)
        livePresentation = button.presentation
        refreshPresentation(button.presentation)
        button.foreground = EditorColorsManager.getInstance().globalScheme.defaultForeground
        button.border = null
        button.putClientProperty("JButton.backgroundColor", Color(0, 0, 0, 0))
        button.putClientProperty(
            "proxyai.refreshPresentation",
            Runnable { refreshPresentation(button.presentation) }
        )
        return button
    }

    override fun createActionPopup(
        group: DefaultActionGroup,
        context: DataContext,
        disposeCallback: Runnable?
    ): JBPopup {
        return ModelListPopups.createPopup(group, context, disposeCallback)
    }

    override fun createPopupActionGroup(
        button: JComponent,
        context: DataContext
    ): DefaultActionGroup {
        val selected = selectedEffort()
        return DefaultActionGroup().apply {
            add(createEffortAction(null, defaultEffortText(), selected == null))
            availableEfforts().forEach { effort ->
                add(createEffortAction(effort, effortLabel(effort), selected == effort))
            }
        }
    }

    private fun createEffortAction(
        effort: String?,
        text: String,
        selected: Boolean
    ): DumbAwareAction {
        return object : DumbAwareAction(
            text,
            null,
            if (selected) AllIcons.Actions.Checked else null
        ) {
            override fun update(event: AnActionEvent) {
                event.presentation.isEnabled = !selected
            }

            override fun actionPerformed(event: AnActionEvent) {
                if (isOpenAI()) {
                    OpenAISettings.getCurrentState().setFeatureReasoningEffort(featureType, effort)
                } else {
                    AnthropicSettings.getCurrentState().setFeatureReasoningEffort(featureType, effort)
                }
                livePresentation?.let(::refreshPresentation)
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }
        }
    }

    private fun refreshPresentation(presentation: Presentation) {
        val effort = if (isOpenAI()) {
            OpenAISettings.getCurrentState().getEffectiveReasoningEffort(featureType)
        } else {
            AnthropicSettings.getCurrentState().getEffectiveReasoningEffort(featureType)
                ?: modelDefaultEffort()
        }
        presentation.text = CodeGPTBundle.get(
            "toolwindow.reasoningEffort.summary",
            effort?.let(::effortLabel) ?: CodeGPTBundle.get("toolwindow.reasoningEffort.default")
        )
        presentation.description = CodeGPTBundle.get("toolwindow.reasoningEffort.description")
    }

    private fun isOpenAI(): Boolean {
        return runCatching {
            ModelSettings.getInstance().getModelSelectionForFeature(featureType).provider
        }.getOrNull() == ServiceType.OPENAI
    }

    private fun availableEfforts(): List<String> {
        return if (isOpenAI()) OPENAI_EFFORTS else ANTHROPIC_EFFORTS
    }

    private fun selectedEffort(): String? {
        val efforts = if (isOpenAI()) {
            OpenAISettings.getCurrentState().featureReasoningEfforts
        } else {
            AnthropicSettings.getCurrentState().featureReasoningEfforts
        }
        return efforts[featureType.name]?.takeIf { it.isNotBlank() }
    }

    private fun defaultEffortText(): String {
        if (isOpenAI()) {
            return CodeGPTBundle.get("toolwindow.reasoningEffort.default")
        }
        val defaultEffort = AnthropicSettings.getCurrentState().reasoningEffort
            ?.takeIf { it.isNotBlank() }
            ?: modelDefaultEffort()
        val label = CodeGPTBundle.get("toolwindow.reasoningEffort.default")
        return defaultEffort?.let { "$label (${effortLabel(it)})" } ?: label
    }

    private fun modelDefaultEffort(): String? {
        val model = runCatching {
            ModelSettings.getInstance().getModelSelectionForFeature(featureType).llmModel
        }.getOrNull() ?: return null
        return CodeGPTAnthropicLLMClient.defaultEffort(model)
    }

    private fun effortLabel(effort: String): String {
        return CodeGPTBundle.get("toolwindow.reasoningEffort.$effort")
    }
}
