package consulo.xml.codeInsight.hint.api.impls;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xhtml.language.XHTMLLanguage;


/**
 * @author VISTALL
 * @since 2022-07-05
 */
@ExtensionImpl
public class XHtmlParameterInfoHandler extends XmlBasedParameterInfoHandler {
    @Override
    public Language getLanguage() {
        return XHTMLLanguage.INSTANCE;
    }
}
