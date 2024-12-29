package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XMLBasedExternalAnnotator;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class HtmlExternalAnnotator extends XMLBasedExternalAnnotator
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
