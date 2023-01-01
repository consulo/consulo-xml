package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedUnwrapDescriptor;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jul-22
 */
@ExtensionImpl
public class XHtmlUnwrapDescriptor extends XmlBasedUnwrapDescriptor
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
