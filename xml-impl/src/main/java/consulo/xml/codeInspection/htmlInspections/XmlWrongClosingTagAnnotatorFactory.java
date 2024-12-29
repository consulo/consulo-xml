package consulo.xml.codeInspection.htmlInspections;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.xml.lang.xml.XMLLanguage;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlWrongClosingTagAnnotatorFactory implements AnnotatorFactory {
  @Nullable
  @Override
  public Annotator createAnnotator() {
    return new XmlWrongClosingTagNameInspection();
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }
}
