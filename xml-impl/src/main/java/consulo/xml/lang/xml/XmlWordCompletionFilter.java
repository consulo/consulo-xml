package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedWordCompletionFilter;
import consulo.xml.language.XMLLanguage;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XmlWordCompletionFilter extends XmlBasedWordCompletionFilter
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
