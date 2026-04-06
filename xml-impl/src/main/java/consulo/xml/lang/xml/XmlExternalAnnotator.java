package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XMLBasedExternalAnnotator;
import consulo.xml.language.XMLLanguage;


/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlExternalAnnotator extends XMLBasedExternalAnnotator
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
