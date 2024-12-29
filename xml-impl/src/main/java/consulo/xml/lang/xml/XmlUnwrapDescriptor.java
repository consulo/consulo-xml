package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedUnwrapDescriptor;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jul-22
 */
@ExtensionImpl
public class XmlUnwrapDescriptor extends XmlBasedUnwrapDescriptor
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
