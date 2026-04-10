package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xhtml.language.XHTMLLanguage;
import consulo.xml.lang.base.XmlBasedFindUsagesProvider;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XHtmlFindUsagesProvider extends XmlBasedFindUsagesProvider
{
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
