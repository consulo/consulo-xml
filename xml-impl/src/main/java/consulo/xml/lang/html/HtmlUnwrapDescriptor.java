package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedUnwrapDescriptor;


/**
 * @author VISTALL
 * @since 14-Jul-22
 */
@ExtensionImpl
public class HtmlUnwrapDescriptor extends XmlBasedUnwrapDescriptor
{
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
