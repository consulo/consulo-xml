/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.lang.xml;

import consulo.ide.impl.idea.codeInsight.unwrap.LanguageUnwrappers;
import consulo.language.Language;
import consulo.language.editor.refactoring.unwrap.UnwrapDescriptor;
import consulo.language.editor.refactoring.unwrap.Unwrapper;
import consulo.codeEditor.Editor;
import consulo.util.lang.Pair;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlTag;
import consulo.project.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class XmlUnwrapDescriptor implements UnwrapDescriptor {
  public List<Pair<PsiElement, Unwrapper>> collectUnwrappers(Project project, Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset();

    PsiElement e1 = file.findElementAt(offset);
    if (e1 != null) {
      Language language = e1.getParent().getLanguage();
      if (language != file.getLanguage()) {
        UnwrapDescriptor unwrapDescriptor = LanguageUnwrappers.INSTANCE.forLanguage(language);
        if (unwrapDescriptor != null && !(unwrapDescriptor instanceof XmlUnwrapDescriptor)) {
          return unwrapDescriptor.collectUnwrappers(project, editor, file);
        }
      }
    }

    List<Pair<PsiElement, Unwrapper>> result = new ArrayList<Pair<PsiElement, Unwrapper>>();

    FileViewProvider viewProvider = file.getViewProvider();

    for (Language language : viewProvider.getLanguages()) {
      UnwrapDescriptor unwrapDescriptor = LanguageUnwrappers.INSTANCE.forLanguage(language);
      if (unwrapDescriptor instanceof XmlUnwrapDescriptor) {
        PsiElement e = viewProvider.findElementAt(offset, language);

        PsiElement tag = PsiTreeUtil.getParentOfType(e, XmlTag.class);
        while (tag != null) {
          if (XmlChildRole.START_TAG_NAME_FINDER.findChild(tag.getNode()) != null) { // Exclude implicit tags suck as 'jsp:root'
            result.add(new Pair<PsiElement, Unwrapper>(tag, new XmlEnclosingTagUnwrapper()));
          }
          tag = PsiTreeUtil.getParentOfType(tag, XmlTag.class);
        }
      }
    }

    Collections.sort(result, new Comparator<Pair<PsiElement, Unwrapper>>() {
      @Override
      public int compare(Pair<PsiElement, Unwrapper> o1, Pair<PsiElement, Unwrapper> o2) {
        return o2.first.getTextOffset() - o1.first.getTextOffset();
      }
    });

    return result;
  }

  public boolean showOptionsDialog() {
    return true;
  }

  public boolean shouldTryToRestoreCaretPosition() {
    return false;
  }
}
