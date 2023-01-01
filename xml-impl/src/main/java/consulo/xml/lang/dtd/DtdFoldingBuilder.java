package consulo.xml.lang.dtd;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedFoldingBuilder;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class DtdFoldingBuilder extends XmlBasedFoldingBuilder
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return DTDLanguage.INSTANCE;
	}
}
