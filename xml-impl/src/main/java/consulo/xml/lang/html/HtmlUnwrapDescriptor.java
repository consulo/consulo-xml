package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedUnwrapDescriptor;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jul-22
 */
@ExtensionImpl
public class HtmlUnwrapDescriptor extends XmlBasedUnwrapDescriptor
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
