package com.intellij.spellchecker.xml;

import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.SuppressibleSpellcheckingStrategy;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import javax.annotation.Nonnull;

/**
 * @author Sergey Evdokimov
 */
public class XmlSpellcheckingStrategy extends SuppressibleSpellcheckingStrategy {

  @Override
  public boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String name) {
    DomElement domElement = DomUtil.getDomElement(element);
    if (domElement != null) {
      if (domElement.getAnnotation(NoSpellchecking.class) != null) {
        return true;
      }
    }

    return false;
  }

  @Override
  public SuppressQuickFix[] getSuppressActions(@Nonnull PsiElement element, @Nonnull String name) {
    return SuppressQuickFix.EMPTY_ARRAY;
  }
}
