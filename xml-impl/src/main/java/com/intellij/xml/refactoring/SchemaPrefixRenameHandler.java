/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.xml.refactoring;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.language.editor.refactoring.rename.inplace.VariableInplaceRenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import com.intellij.psi.impl.source.xml.PossiblePrefixReference;
import com.intellij.psi.impl.source.xml.SchemaPrefix;
import consulo.language.editor.refactoring.rename.inplace.VariableInplaceRenamer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public class SchemaPrefixRenameHandler extends VariableInplaceRenameHandler {
  
  @Override
  protected boolean isAvailable(PsiElement element, Editor editor, PsiFile file) {
    PossiblePrefixReference ref = getReference(file, editor);
    return ref != null && ref.resolve() instanceof SchemaPrefix;
  }

  @Nullable
  private static PossiblePrefixReference getReference(PsiFile file, Editor editor) {
    if (file != null && editor != null) {
      int offset = editor.getCaretModel().getOffset();
      PsiReference reference = file.findReferenceAt(offset);
      if (reference instanceof PossiblePrefixReference) {
        return (PossiblePrefixReference)reference;
      }
    }
    return null;

  }

  @Override
  protected VariableInplaceRenamer createRenamer(@Nonnull PsiElement elementToRename, Editor editor) {
    PossiblePrefixReference reference = getReference(elementToRename.getContainingFile(), editor);
    if (reference != null) {
      PsiElement prefix = reference.resolve();
      if (prefix instanceof SchemaPrefix) {
        return new VariableInplaceRenamer((PsiNamedElement)prefix, editor) {
          @Override
          protected void addReferenceAtCaret(Collection<PsiReference> refs) {}

          @Override
          protected boolean isReferenceAtCaret(PsiElement selectedElement, PsiReference ref) {
            return false;
          }
        };
      }
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      System.out.println("Reference: " + reference);
      if (reference != null) {
        System.out.println("Resolved: " + reference.resolve());
      }
    }
    return null;
  }
}
