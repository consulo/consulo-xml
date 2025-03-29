package consulo.xml.codeInsight.hint.api.impls;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.html.HTMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-07-05
 */
@ExtensionImpl
public class HtmlParameterInfoHandler extends XmlBasedParameterInfoHandler {
    @Nonnull
    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
