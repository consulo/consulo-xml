package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.codeInsight.completion.base.XmlBasedSmartEnterProcessor;
import consulo.html.language.HTMLLanguage;


/**
 * @author VISTALL
 * @since 2022-07-31
 */
@ExtensionImpl
public class HtmlSmartEnterProcessor extends XmlBasedSmartEnterProcessor {
    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
