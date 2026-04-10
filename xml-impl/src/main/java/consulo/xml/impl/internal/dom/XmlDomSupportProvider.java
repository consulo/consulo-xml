package consulo.xml.impl.internal.dom;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.dom.DomSupportProvider;
import consulo.xml.language.XmlFileType;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
@ExtensionImpl
public class XmlDomSupportProvider implements DomSupportProvider {
    @Override
    public FileType getFileType() {
        return XmlFileType.INSTANCE;
    }
}
