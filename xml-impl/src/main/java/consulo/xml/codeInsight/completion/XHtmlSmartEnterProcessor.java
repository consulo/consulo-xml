package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.codeInsight.completion.base.XmlBasedSmartEnterProcessor;
import consulo.xhtml.language.XHTMLLanguage;


/**
 * @author VISTALL
 * @since 2022-07-31
 */
@ExtensionImpl
public class XHtmlSmartEnterProcessor extends XmlBasedSmartEnterProcessor {
    @Override
    public Language getLanguage() {
        return XHTMLLanguage.INSTANCE;
    }
}
