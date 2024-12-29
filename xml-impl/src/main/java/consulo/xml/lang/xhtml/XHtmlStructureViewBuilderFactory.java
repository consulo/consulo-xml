package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedStructureViewBuilderFactory;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30-Jul-22
 */
@ExtensionImpl
public class XHtmlStructureViewBuilderFactory extends XmlBasedStructureViewBuilderFactory
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
