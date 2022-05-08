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
package consulo.xml.codeInsight.daemon.impl.quickfix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.ide.impl.idea.codeInsight.intention.impl.BaseIntentionAction;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.DependentNSReference;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.URLReference;
import consulo.xml.psi.xml.XmlFile;
import consulo.language.util.IncorrectOperationException;
import com.intellij.xml.XmlBundle;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;

/**
 * @author mike
 */
abstract class BaseExtResourceAction extends BaseIntentionAction {

  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    if (!(file instanceof XmlFile)) return false;

    int offset = editor.getCaretModel().getOffset();
    String uri = findUri(file, offset);
    if (uri == null || !isAcceptableUri(uri)) return false;

    setText(XmlBundle.message(getQuickFixKeyId()));
    return true;
  }

  protected boolean isAcceptableUri(final String uri) {
    return true;
  }

  protected abstract String getQuickFixKeyId();

  @Nonnull
  public String getFamilyName() {
    return XmlBundle.message(getQuickFixKeyId());
  }

  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws consulo.language.util.IncorrectOperationException {
    int offset = editor.getCaretModel().getOffset();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final String uri = findUri(file, offset);
    if (uri == null) return;

    doInvoke(file, offset, uri, editor);
  }

  protected abstract void doInvoke(final @Nonnull PsiFile file, final int offset, final @Nonnull String uri, final Editor editor)
    throws IncorrectOperationException;

  @Nullable
  public static String findUri(PsiFile file, int offset) {
    PsiReference currentRef = file.getViewProvider().findReferenceAt(offset, file.getLanguage());
    if (currentRef == null) currentRef = file.getViewProvider().findReferenceAt(offset);
    if (currentRef instanceof URLReference ||
        currentRef instanceof DependentNSReference) {
      return currentRef.getCanonicalText();
    }
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ManuallySetupExtResourceAction;
  }
}