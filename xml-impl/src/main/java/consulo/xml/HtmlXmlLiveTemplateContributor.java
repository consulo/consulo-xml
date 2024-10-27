package consulo.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInsight.template.HtmlContextType;
import consulo.xml.codeInsight.template.XmlContextType;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class HtmlXmlLiveTemplateContributor implements LiveTemplateContributor {
  @Override
  @Nonnull
  public String groupId() {
    return "htmlXml";
  }

  @Override
  @Nonnull
  public LocalizeValue groupName() {
    return LocalizeValue.localizeTODO("HTML & XML");
  }

  @Override
  public void contribute(@Nonnull Factory factory) {
    try(Builder builder = factory.newBuilder("htmlxmlT", "T", "<$TAG$>$SELECTION$</$TAGNAME$>\r\n", CodeInsightLocalize.livetemplateDescriptionSurroundTag())) {
      builder.withReformat();

      builder.withVariable("TAG", "", "", true);
      builder.withVariable("SELECTION", "", "", false);
      builder.withVariable("TAGNAME", "firstWord(TAG)", "\"\"", false);

      builder.withContext(XmlContextType.class, true);
      builder.withContext(HtmlContextType.class, false);
    }

    try(Builder builder = factory.newBuilder("htmlxmlT2", "T2", "<$TAG$>$SELECTION$</$TAGNAME$>", CodeInsightLocalize.livetemplateDescriptionSurroundTagInHtmlorjsp())) {
      builder.withVariable("TAG", "", "", true);
      builder.withVariable("SELECTION", "", "", false);
      builder.withVariable("TAGNAME", "firstWord(TAG)", "\"\"", false);

      builder.withContext(XmlContextType.class, false);
      builder.withContext(HtmlContextType.class, true);
    }

    try(Builder builder = factory.newBuilder("htmlxmlCD", "CD", "<![CDATA[\n"
        + "$SELECTION$\n"
        + "]]>", CodeInsightLocalize.livetemplateDescriptionSurroundCdataInXmlorhtmlorjsp())) {
      builder.withVariable("SELECTION", "", "", false);

      builder.withContext(XmlContextType.class, true);
      builder.withContext(HtmlContextType.class, true);
    }

  }
}
