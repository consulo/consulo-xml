package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedStructureViewBuilderFactory;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30-Jul-22
 */
@ExtensionImpl
public class HtmlStructureViewBuilderFactory extends XmlBasedStructureViewBuilderFactory
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
