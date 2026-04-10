package consulo.xml.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xhtml.language.XHtmlFileType;


/**
 * @author VISTALL
 * @since 2022-08-02
 */
@ExtensionImpl
public class XHtmlQuoteHandler extends HtmlQuoteHandler {
    @Override
    public FileType getFileType() {
        return XHtmlFileType.INSTANCE;
    }
}
