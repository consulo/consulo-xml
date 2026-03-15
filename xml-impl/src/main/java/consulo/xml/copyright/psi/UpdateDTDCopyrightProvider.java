package consulo.xml.copyright.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.DTDFileType;


/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class UpdateDTDCopyrightProvider extends UpdateXmlBasedCopyrightsProvider {
  @Override
  public FileType getFileType() {
    return DTDFileType.INSTANCE;
  }
}
