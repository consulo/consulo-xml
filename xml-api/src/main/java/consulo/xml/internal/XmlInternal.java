package consulo.xml.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlTag;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface XmlInternal {
    PsiElement[] getIncludedTags(XmlTag xincludeTag);
}
