package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedFoldingBuilder;


/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlFoldingBuilder extends XmlBasedFoldingBuilder
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
