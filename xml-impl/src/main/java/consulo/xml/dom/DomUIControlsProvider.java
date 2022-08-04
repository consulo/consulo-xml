package consulo.xml.dom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.xml.util.xml.ui.DomUIFactory;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DomUIControlsProvider
{
	void register(DomUIFactory domUIFactory);
}
