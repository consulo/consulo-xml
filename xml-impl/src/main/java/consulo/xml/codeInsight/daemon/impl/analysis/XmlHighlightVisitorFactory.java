package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.editor.rawHighlight.HighlightVisitorFactory;
import consulo.language.psi.PsiFile;
import consulo.xml.language.psi.XmlFile;


/**
 * @author VISTALL
 * @since 2023-03-25
 */
@ExtensionImpl
public class XmlHighlightVisitorFactory implements HighlightVisitorFactory {
    @Override
    public boolean suitableForFile(PsiFile file) {
        if (file instanceof XmlFile) {
            return true;
        }

        for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
            if (psiFile instanceof XmlFile) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HighlightVisitor createVisitor() {
        return new XmlHighlightVisitor();
    }
}
