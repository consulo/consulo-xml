package consulo.xml.codeInsight.hint.api.impls;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xml.XMLLanguage;


/**
 * @author VISTALL
 * @since 2022-07-05
 */
@ExtensionImpl
public class XmlParameterInfoHandler extends XmlBasedParameterInfoHandler {
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
