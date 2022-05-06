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

import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.psi.xml.XmlTag;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.analysis.ErrorQuickFixProvider;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.QuickFixAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

public class XmlErrorQuickFixProvider implements ErrorQuickFixProvider {
  @NonNls private static final String AMP_ENTITY = "&amp;";

  public void registerErrorQuickFix(final PsiErrorElement element, final HighlightInfo highlightInfo) {
    if (PsiTreeUtil.getParentOfType(element, XmlTag.class) != null) {
      registerXmlErrorQuickFix(element,highlightInfo);
    }
  }

  private static void registerXmlErrorQuickFix(final PsiErrorElement element, final HighlightInfo highlightInfo) {
    final String text = element.getErrorDescription();
    if (text != null && text.startsWith(XmlErrorMessages.message("unescaped.ampersand"))) {
      QuickFixAction.registerQuickFixAction(highlightInfo, new IntentionAction() {
        @Nonnull
        public String getText() {
          return XmlErrorMessages.message("escape.ampersand.quickfix");
        }

        @Nonnull
        public String getFamilyName() {
          return getText();
        }

        public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
          return true;
        }

        public void invoke(@Nonnull Project project, Editor editor, PsiFile file) {
          if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
          final int textOffset = element.getTextOffset();
          editor.getDocument().replaceString(textOffset,textOffset + 1,AMP_ENTITY);
        }

        public boolean startInWriteAction() {
          return true;
        }
      });
    }
  }
}
