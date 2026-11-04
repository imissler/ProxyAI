package ee.carlrobert.codegpt.settings.agents

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import ee.carlrobert.codegpt.CodeGPTBundle
import ee.carlrobert.codegpt.settings.configuration.AutoCompactSettings
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings

class AgentConfigurable : BoundConfigurable("Agent") {

    override fun createPanel(): DialogPanel {
        val state = ConfigurationSettings.getState()
        return panel {
            group(CodeGPTBundle.get("agentConfigurable.section.compaction.title")) {
                lateinit var autoCompact: com.intellij.ui.dsl.builder.Cell<com.intellij.ui.components.JBCheckBox>
                row {
                    autoCompact = checkBox(CodeGPTBundle.get("agentConfigurable.autoCompact.title"))
                        .bindSelected(state::autoCompactEnabled)
                        .comment(CodeGPTBundle.get("agentConfigurable.autoCompact.description"))
                }
                row(CodeGPTBundle.get("agentConfigurable.autoCompactThreshold.title")) {
                    spinner(
                        AutoCompactSettings.MIN_THRESHOLD_PERCENT..AutoCompactSettings.MAX_THRESHOLD_PERCENT,
                        5
                    )
                        .bindIntValue(state::autoCompactThresholdPercent)
                        .enabledIf(autoCompact.selected)
                        .comment(
                            CodeGPTBundle.get(
                                "agentConfigurable.autoCompactThreshold.description",
                                AutoCompactSettings.DEFAULT_THRESHOLD_PERCENT
                            )
                        )
                }
            }
        }
    }
}
