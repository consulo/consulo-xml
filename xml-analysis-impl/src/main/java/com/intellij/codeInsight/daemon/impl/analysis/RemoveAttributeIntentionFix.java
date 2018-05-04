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
package com.intellij.codeInsight.daemon.impl.analysis;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;

/**
 * @author Maxim.Mossienko
 */
public class RemoveAttributeIntentionFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private final String myLocalName;

  public RemoveAttributeIntentionFix(final String localName, final @Nonnull XmlAttribute attribute) {
    super(attribute);
    myLocalName = localName;
  }

  @Nonnull
  public String getText() {
    return XmlErrorMessages.message("remove.attribute.quickfix.text", myLocalName);
  }

  @Nonnull
  public String getFamilyName() {
    return XmlErrorMessages.message("remove.attribute.quickfix.family");
  }

  @Override
  public void invoke(@Nonnull Project project,
                     @Nonnull PsiFile file,
                     @Nullable("is null when called from inspection") Editor editor,
                     @Nonnull PsiElement startElement,
                     @Nonnull PsiElement endElement) {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    PsiElement next = findNextAttribute((XmlAttribute)startElement);
    startElement.delete();

    if (next != null && editor != null) {
      editor.getCaretModel().moveToOffset(next.getTextRange().getStartOffset());
    }
  }

  @Nullable
  private static PsiElement findNextAttribute(final XmlAttribute attribute) {
    PsiElement nextSibling = attribute.getNextSibling();
    while (nextSibling != null) {
      if (nextSibling instanceof XmlAttribute) return nextSibling;
      nextSibling =  nextSibling.getNextSibling();
    }
    return null;
  }

  public boolean startInWriteAction() {
    return true;
  }
}
