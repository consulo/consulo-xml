package consulo.xml.psi.impl.source.xml.behavior;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xml.XMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class HtmlXmlPsiPolicy extends EncodeEachSymbolPolicy
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
