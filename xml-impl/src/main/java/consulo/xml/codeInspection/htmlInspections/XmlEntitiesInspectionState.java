package consulo.xml.codeInspection.htmlInspections;

import consulo.component.persist.PersistentStateComponent;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11/03/2023
 */
public class XmlEntitiesInspectionState extends BaseXmlEntitiesInspectionState<XmlEntitiesInspectionState> implements PersistentStateComponent<XmlEntitiesInspectionState> {
  public XmlEntitiesInspectionState() {
  }

  public XmlEntitiesInspectionState(@Nonnull LocalizeValue labelText) {
    super(labelText);
  }

  public XmlEntitiesInspectionState(@Nonnull LocalizeValue labelText, String... defaultValues) {
    super(labelText, defaultValues);
  }
}
