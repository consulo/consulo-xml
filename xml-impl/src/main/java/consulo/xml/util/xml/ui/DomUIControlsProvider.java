package consulo.xml.util.xml.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DomUIControlsProvider {
    void register(DomUIFactory domUIFactory);
}
