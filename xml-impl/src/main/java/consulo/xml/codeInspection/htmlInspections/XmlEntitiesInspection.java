package consulo.xml.codeInspection.htmlInspections;

import org.jetbrains.annotations.NonNls;

/**
 * @author anna
 * @since 2005-12-16
 */
@Deprecated
public interface XmlEntitiesInspection {
  @NonNls
  String BOOLEAN_ATTRIBUTE_SHORT_NAME = "HtmlUnknownBooleanAttribute";
  @NonNls
  String ATTRIBUTE_SHORT_NAME = "HtmlUnknownAttribute";
  @NonNls
  String TAG_SHORT_NAME = "HtmlUnknownTag";
  @NonNls
  String REQUIRED_ATTRIBUTES_SHORT_NAME = "RequiredAttributes";
}
