package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl(id = "xhtmlText")
public class XHtmlTextCompletionConfidence extends HtmlTextCompletionConfidence {
    @Nonnull
    @Override
    public Language getLanguage() {
        return XHTMLLanguage.INSTANCE;
    }
}
