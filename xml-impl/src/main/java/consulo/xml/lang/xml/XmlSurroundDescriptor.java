package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedSurroundDescriptor;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XmlSurroundDescriptor extends XmlBasedSurroundDescriptor
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
