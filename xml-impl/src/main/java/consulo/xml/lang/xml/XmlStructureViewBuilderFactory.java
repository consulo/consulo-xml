package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedStructureViewBuilderFactory;
import consulo.xml.language.XMLLanguage;


/**
 * @author VISTALL
 * @since 30-Jul-22
 */
@ExtensionImpl
public class XmlStructureViewBuilderFactory extends XmlBasedStructureViewBuilderFactory
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
