package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedUnwrapDescriptor;
import consulo.xml.language.XMLLanguage;


/**
 * @author VISTALL
 * @since 14-Jul-22
 */
@ExtensionImpl
public class XmlUnwrapDescriptor extends XmlBasedUnwrapDescriptor
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
