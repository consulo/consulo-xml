package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xhtml.language.XHTMLLanguage;
import consulo.xml.lang.base.XmlBasedStructureViewBuilderFactory;


/**
 * @author VISTALL
 * @since 30-Jul-22
 */
@ExtensionImpl
public class XHtmlStructureViewBuilderFactory extends XmlBasedStructureViewBuilderFactory
{
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
