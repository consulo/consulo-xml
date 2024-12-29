package consulo.xml.copyright.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XmlFileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class UpdateXmlCopyrightsProvider extends UpdateXmlBasedCopyrightsProvider {
  @Nonnull
  @Override
  public FileType getFileType() {
    return XmlFileType.INSTANCE;
  }
}
