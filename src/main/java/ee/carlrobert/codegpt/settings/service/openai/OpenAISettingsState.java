package ee.carlrobert.codegpt.settings.service.openai;

import ee.carlrobert.codegpt.settings.service.FeatureType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class OpenAISettingsState {

  private String organization = "";
  private String model = "gpt-4.1";
  private boolean codeCompletionsEnabled = false;
  private Map<String, String> featureReasoningEfforts = new HashMap<>();

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public boolean isCodeCompletionsEnabled() {
    return codeCompletionsEnabled;
  }

  public void setCodeCompletionsEnabled(boolean codeCompletionsEnabled) {
    this.codeCompletionsEnabled = codeCompletionsEnabled;
  }

  public Map<String, String> getFeatureReasoningEfforts() {
    return featureReasoningEfforts;
  }

  public void setFeatureReasoningEfforts(Map<String, String> featureReasoningEfforts) {
    this.featureReasoningEfforts = featureReasoningEfforts;
  }

  /**
   * Effort chosen for the feature, or null to keep the model default.
   */
  public @Nullable String getEffectiveReasoningEffort(FeatureType featureType) {
    var effort = featureReasoningEfforts.get(featureType.name());
    return effort == null || effort.isBlank() ? null : effort;
  }

  public void setFeatureReasoningEffort(FeatureType featureType, @Nullable String effort) {
    var updated = new HashMap<>(featureReasoningEfforts);
    if (effort == null || effort.isBlank()) {
      updated.remove(featureType.name());
    } else {
      updated.put(featureType.name(), effort);
    }
    featureReasoningEfforts = updated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenAISettingsState that = (OpenAISettingsState) o;
    return Objects.equals(organization, that.organization)
        && Objects.equals(model, that.model)
        && codeCompletionsEnabled == that.codeCompletionsEnabled
        && Objects.equals(featureReasoningEfforts, that.featureReasoningEfforts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(organization, model, codeCompletionsEnabled, featureReasoningEfforts);
  }
}
