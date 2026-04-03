package consulo.xml.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.action.LanguageQuoteHandler;
import consulo.xml.lang.xml.XMLLanguage;


/**
 * @author VISTALL
 * @since 2022-07-05
 */
@ExtensionImpl
public class XmlLanguageQuoteHandler extends XmlBasedQuoteHandler implements LanguageQuoteHandler {
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
