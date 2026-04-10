package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedFindUsagesProvider;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class HtmlFindUsagesProvider extends XmlBasedFindUsagesProvider
{
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
