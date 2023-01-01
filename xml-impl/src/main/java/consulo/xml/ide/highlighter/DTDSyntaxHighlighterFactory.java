package consulo.xml.ide.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.xml.lang.dtd.DTDLanguage;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14.02.2015
 */
@ExtensionImpl
public class DTDSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
  @Nonnull
  @Override
  protected SyntaxHighlighter createHighlighter() {
    return new XmlFileHighlighter(true);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return DTDLanguage.INSTANCE;
  }
}
