package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XMLBasedExternalAnnotator;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XHtmlExternalAnnotator extends XMLBasedExternalAnnotator
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
