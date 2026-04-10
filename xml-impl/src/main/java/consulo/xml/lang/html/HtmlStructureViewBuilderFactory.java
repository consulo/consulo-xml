package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedStructureViewBuilderFactory;


/**
 * @author VISTALL
 * @since 30-Jul-22
 */
@ExtensionImpl
public class HtmlStructureViewBuilderFactory extends XmlBasedStructureViewBuilderFactory
{
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
