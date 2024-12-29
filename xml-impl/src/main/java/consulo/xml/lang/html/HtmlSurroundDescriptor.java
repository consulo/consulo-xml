package consulo.xml.lang.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.base.XmlBasedSurroundDescriptor;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl(id = "html-xml")
public class HtmlSurroundDescriptor extends XmlBasedSurroundDescriptor
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
