package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedFindUsagesProvider;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class HtmlFindUsagesProvider extends XmlBasedFindUsagesProvider
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
