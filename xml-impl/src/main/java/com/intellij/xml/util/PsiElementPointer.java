package com.intellij.xml.util;

import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public interface PsiElementPointer {
    @Nullable
    PsiElement getPsiElement();
}
