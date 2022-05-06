package com.intellij.xml.refactoring;

import consulo.ide.impl.idea.lang.refactoring.InlineHandler;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlInlineHandler implements InlineHandler {
  @Override
  public Settings prepareInlineElement(PsiElement element, Editor editor, boolean invokedOnReference) {
    return null;
  }

  @Override
  public void removeDefinition(PsiElement element, Settings settings) {
  }

  @Override
  public Inliner createInliner(PsiElement element, Settings settings) {
    return null;
  }
}
