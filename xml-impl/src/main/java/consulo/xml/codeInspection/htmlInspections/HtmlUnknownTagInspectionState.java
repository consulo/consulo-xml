package consulo.xml.codeInspection.htmlInspections;

import consulo.component.persist.PersistentStateComponent;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11/03/2023
 */
public class HtmlUnknownTagInspectionState extends BaseHtmlEntitiesInspectionState<HtmlUnknownTagInspectionState>
  implements PersistentStateComponent<HtmlUnknownTagInspectionState> {
  private static final String[] ourDefaultValues = {"nobr", "noembed", "comment", "noscript", "embed", "script"};

  public HtmlUnknownTagInspectionState() {
    super(LocalizeValue.empty(), ourDefaultValues);
  }

  public HtmlUnknownTagInspectionState(@Nonnull LocalizeValue labelText) {
    super(labelText, ourDefaultValues);
  }
}
