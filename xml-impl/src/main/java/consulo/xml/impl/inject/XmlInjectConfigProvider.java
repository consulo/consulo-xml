package consulo.xml.impl.inject;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.psi.injection.InjectionConfigProvider;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlInjectConfigProvider implements InjectionConfigProvider
{
	@Nonnull
	@Override
	public String getConfigFilePath()
	{
		return "/resources/xmlInjections-html.xml";
	}
}
