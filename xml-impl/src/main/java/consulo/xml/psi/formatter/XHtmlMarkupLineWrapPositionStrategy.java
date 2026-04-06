package consulo.xml.psi.formatter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XHtmlMarkupLineWrapPositionStrategy extends MarkupLineWrapPositionStrategy
{
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
