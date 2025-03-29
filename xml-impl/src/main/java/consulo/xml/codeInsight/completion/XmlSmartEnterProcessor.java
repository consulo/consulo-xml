package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.codeInsight.completion.base.XmlBasedSmartEnterProcessor;
import consulo.xml.lang.xml.XMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-07-31
 */
@ExtensionImpl
public class XmlSmartEnterProcessor extends XmlBasedSmartEnterProcessor {
    @Nonnull
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
