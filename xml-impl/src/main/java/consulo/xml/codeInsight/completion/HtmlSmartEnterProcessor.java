package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.codeInsight.completion.base.XmlBasedSmartEnterProcessor;
import consulo.xml.lang.html.HTMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class HtmlSmartEnterProcessor extends XmlBasedSmartEnterProcessor {
    @Nonnull
    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
