package consulo.xml.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.action.FileQuoteHandler;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XmlFileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-07-05
 */
@ExtensionImpl
public class XmlFileQuoteHandler extends XmlBasedQuoteHandler implements FileQuoteHandler {
    @Nonnull
    @Override
    public FileType getFileType() {
        return XmlFileType.INSTANCE;
    }
}
