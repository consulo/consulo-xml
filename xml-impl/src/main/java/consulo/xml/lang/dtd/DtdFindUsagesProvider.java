package consulo.xml.lang.dtd;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedFindUsagesProvider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class DtdFindUsagesProvider extends XmlBasedFindUsagesProvider
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return DTDLanguage.INSTANCE;
	}
}
