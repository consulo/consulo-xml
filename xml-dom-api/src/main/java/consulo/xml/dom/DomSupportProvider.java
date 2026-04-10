package consulo.xml.dom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DomSupportProvider {
    FileType getFileType();
}
