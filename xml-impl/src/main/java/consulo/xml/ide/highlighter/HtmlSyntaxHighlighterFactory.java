package consulo.xml.ide.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.html.language.HTMLLanguage;
import jakarta.inject.Inject;


/**
 * @author VISTALL
 * @since 14.02.2015
 */
@ExtensionImpl
public class HtmlSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
  private final Application myApplication;

  @Inject
  public HtmlSyntaxHighlighterFactory(Application application) {
    myApplication = application;
  }

  @Override
  protected SyntaxHighlighter createHighlighter() {
    return new HtmlFileHighlighter(myApplication);
  }

  @Override
  public Language getLanguage() {
    return HTMLLanguage.INSTANCE;
  }
}
