package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedFoldingBuilder;


/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class HtmlFoldingBuilder extends XmlBasedFoldingBuilder
{
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
