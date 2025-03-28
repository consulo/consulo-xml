package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.editor.rawHighlight.HighlightVisitorFactory;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlFile;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25/03/2023
 */
@ExtensionImpl
public class XmlHighlightVisitorFactory implements HighlightVisitorFactory {
    @Override
    public boolean suitableForFile(@Nonnull PsiFile file) {
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

    @Nonnull
    @Override
    public HighlightVisitor createVisitor() {
        return new XmlHighlightVisitor();
    }
}
