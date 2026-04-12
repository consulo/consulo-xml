package consulo.xml.dom;

import consulo.language.pom.PomRenameableTarget;
import consulo.language.pom.PsiDeclaredTarget;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiTarget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public interface DomTarget extends PsiTarget, PsiDeclaredTarget, PomRenameableTarget {
    @Nullable
    DomElement getDomElement();

    int getTextOffset();
}
