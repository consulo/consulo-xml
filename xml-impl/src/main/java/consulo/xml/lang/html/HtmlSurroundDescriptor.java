package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedSurroundDescriptor;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl(id = "html-xml")
public class HtmlSurroundDescriptor extends XmlBasedSurroundDescriptor
{
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
