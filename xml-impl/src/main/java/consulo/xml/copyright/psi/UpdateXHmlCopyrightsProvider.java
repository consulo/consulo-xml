package consulo.xml.copyright.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xhtml.language.XHtmlFileType;


/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class UpdateXHmlCopyrightsProvider extends UpdateXmlBasedCopyrightsProvider {
  @Override
  public FileType getFileType() {
    return XHtmlFileType.INSTANCE;
  }
}
