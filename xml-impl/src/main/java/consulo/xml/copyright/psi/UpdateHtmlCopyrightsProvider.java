package consulo.xml.copyright.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.html.language.HtmlFileType;


/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class UpdateHtmlCopyrightsProvider extends UpdateXmlBasedCopyrightsProvider {
  @Override
  public FileType getFileType() {
    return HtmlFileType.INSTANCE;
  }
}
