package consulo.xml.psi.formatter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.language.XMLLanguage;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XmlMarkupLineWrapPositionStrategy extends MarkupLineWrapPositionStrategy
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
