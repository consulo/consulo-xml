package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedWordCompletionFilter;
import consulo.xml.lang.xhtml.XHTMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class HtmlWordCompletionFilter extends XmlBasedWordCompletionFilter
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
