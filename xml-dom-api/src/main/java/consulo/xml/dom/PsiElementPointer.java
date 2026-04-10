package consulo.xml.dom;

import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public interface PsiElementPointer {
    @Nullable
    PsiElement getPsiElement();
}
