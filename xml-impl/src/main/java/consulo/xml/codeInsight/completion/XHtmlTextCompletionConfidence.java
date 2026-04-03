package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;


/**
 * @author VISTALL
 * @since 2022-07-31
 */
@ExtensionImpl(id = "xhtmlText")
public class XHtmlTextCompletionConfidence extends HtmlTextCompletionConfidence {
    @Override
    public Language getLanguage() {
        return XHTMLLanguage.INSTANCE;
    }
}
