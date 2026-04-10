package consulo.xml.impl.internal;

import com.intellij.xml.util.InclusionProvider;
import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiElement;
import consulo.xml.internal.XmlInternal;
import consulo.xml.language.psi.XmlTag;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
@Singleton
@ServiceImpl
public class XmlInternalImpl implements XmlInternal {
    @Override
    public PsiElement[] getIncludedTags(XmlTag xincludeTag) {
        return InclusionProvider.getIncludedTags(xincludeTag);
    }
}
