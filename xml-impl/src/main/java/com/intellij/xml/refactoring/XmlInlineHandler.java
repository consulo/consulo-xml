package com.intellij.xml.refactoring;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.psi.PsiElement;
import consulo.xml.lang.xml.XMLLanguage;

import javax.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
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

    @Nonnull
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
