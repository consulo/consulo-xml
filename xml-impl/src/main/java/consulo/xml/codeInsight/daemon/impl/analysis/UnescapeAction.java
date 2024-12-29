package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.codeEditor.Editor;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.xml.impl.localize.XmlErrorLocalize;

import jakarta.annotation.Nonnull;

class UnescapeAction implements SyntheticIntentionAction {
    private static final String AMP_ENTITY = "&amp;";

    private final PsiErrorElement myElement;

    public UnescapeAction(PsiErrorElement element) {
        myElement = element;
    }

    @Override
    @Nonnull
    public String getText() {
        return XmlErrorLocalize.escapeAmpersandQuickfix().get();
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        final int textOffset = myElement.getTextOffset();
        editor.getDocument().replaceString(textOffset, textOffset + 1, AMP_ENTITY);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
