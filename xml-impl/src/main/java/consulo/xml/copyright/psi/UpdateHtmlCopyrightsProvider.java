package consulo.xml.copyright.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class UpdateHtmlCopyrightsProvider extends UpdateXmlBasedCopyrightsProvider {
  @Nonnull
  @Override
  public FileType getFileType() {
    return HtmlFileType.INSTANCE;
  }
}
