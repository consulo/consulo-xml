package consulo.xml.psi.impl.source.xml.behavior;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XHtmlXmlPsiPolicy extends EncodeEachSymbolPolicy
{
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
