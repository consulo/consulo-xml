package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedWordCompletionFilter;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XHtmlWordCompletionFilter extends XmlBasedWordCompletionFilter
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
