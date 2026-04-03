package consulo.xml.impl.inject;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.inject.advanced.InjectionConfigProvider;

import java.io.InputStream;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlInjectConfigProvider implements InjectionConfigProvider {
    @Override
    public InputStream openConfigFileStream() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/resources/xmlInjections-html.xml");
        return stream;
    }
}
