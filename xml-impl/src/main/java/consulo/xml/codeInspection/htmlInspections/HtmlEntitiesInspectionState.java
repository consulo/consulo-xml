package consulo.xml.codeInspection.htmlInspections;

import consulo.component.persist.PersistentStateComponent;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11/03/2023
 */
public class HtmlEntitiesInspectionState extends BaseHtmlEntitiesInspectionState<HtmlEntitiesInspectionState>
  implements PersistentStateComponent<HtmlEntitiesInspectionState> {
  public HtmlEntitiesInspectionState() {
  }

  public HtmlEntitiesInspectionState(@Nonnull LocalizeValue labelText) {
    super(labelText);
  }

  public HtmlEntitiesInspectionState(@Nonnull LocalizeValue labelText, String... defaultValues) {
    super(labelText, defaultValues);
  }
}
