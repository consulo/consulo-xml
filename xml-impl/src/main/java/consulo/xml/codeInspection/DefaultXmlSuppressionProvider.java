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

package consulo.xml.codeInspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.util.LanguageUndoUtil;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl(order = "last")
public class DefaultXmlSuppressionProvider extends XmlSuppressionProvider {

  public static final String SUPPRESS_MARK = "suppress";

  @Override
  public boolean isProviderAvailable(@Nonnull PsiFile file) {
    return true;
  }

  @Override
  public boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String inspectionId) {
    final XmlTag tag = element instanceof XmlFile ? ((XmlFile) element).getRootTag() : PsiTreeUtil.getContextOfType(element, XmlTag.class, false);
    return tag != null && findSuppression(tag, inspectionId, element) != null;
  }

  @Override
  public void suppressForFile(@Nonnull PsiElement element, @Nonnull String inspectionId) {
    final PsiFile file = element.getContainingFile();
    final XmlDocument document = ((XmlFile) file).getDocument();
    final PsiElement anchor = document != null ? document.getRootTag() : file.findElementAt(0);
    assert anchor != null;
    suppress(file, findFileSuppression(anchor, null, element), inspectionId, anchor.getTextRange().getStartOffset());
  }

  @Override
  public void suppressForTag(@Nonnull PsiElement element, @Nonnull String inspectionId) {
    final XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
    assert tag != null;
    suppress(element.getContainingFile(), findSuppressionLeaf(tag, null, 0), inspectionId, tag.getTextRange().getStartOffset());
  }

  @Nullable
  protected PsiElement findSuppression(final PsiElement anchor, final String id, PsiElement originalElement) {
    final PsiElement element = findSuppressionLeaf(anchor, id, 0);
    if (element != null) return element;

    return findFileSuppression(anchor, id, originalElement);
  }

  @Nullable
  protected PsiElement findFileSuppression(PsiElement anchor, String id, PsiElement originalElement) {
    final PsiFile file = anchor.getContainingFile();
    if (file instanceof XmlFile) {
      final XmlDocument document = ((XmlFile) file).getDocument();
      final XmlTag rootTag = document != null ? document.getRootTag() : null;
      PsiElement leaf = rootTag != null ? rootTag.getPrevSibling() : file.findElementAt(0);
      return findSuppressionLeaf(leaf, id, 0);
    }
    return null;
  }

  @Nullable
  protected PsiElement findSuppressionLeaf(PsiElement leaf, @Nullable final String id, int offset) {
    while (leaf != null && leaf.getTextOffset() >= offset) {
      if (leaf instanceof PsiComment || leaf instanceof XmlProlog || leaf instanceof XmlText) {
        @NonNls String text = leaf.getText();
        if (isSuppressedFor(text, id)) return leaf;
      }
      leaf = leaf.getPrevSibling();
      if (leaf instanceof XmlTag) {
        return null;
      }
    }
    return null;
  }

  private boolean isSuppressedFor(@NonNls final String text, @Nullable final String id) {
    if (!text.contains(getPrefix())) {
      return false;
    }
    if (id == null) {
      return true;
    }
    @NonNls final String[] parts = text.split("[ ,]");
    return ArrayUtil.find(parts, id) != -1 || ArrayUtil.find(parts, XmlSuppressableInspectionTool.ALL) != -1;
  }

  protected void suppress(PsiFile file, final PsiElement suppressionElement, String inspectionId, final int offset) {
    final Project project = file.getProject();
    if (!CommonRefactoringUtil.checkReadOnlyStatus(file)) {
      return;
    }
    final Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
    assert doc != null;

    if (suppressionElement != null) {
      final TextRange textRange = suppressionElement.getTextRange();
      String text = suppressionElement.getText();
      final String suppressionText = getSuppressionText(inspectionId, text);
      doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), suppressionText);
    } else {
      final String suppressionText = getSuppressionText(inspectionId, null);
      doc.insertString(offset, suppressionText);
      CodeStyleManager.getInstance(project).adjustLineIndent(doc, offset + suppressionText.length());
      LanguageUndoUtil.markPsiFileForUndo(file);
    }
  }

  protected String getSuppressionText(String inspectionId, @Nullable String originalText) {
    if (originalText == null) {
      return getPrefix() + inspectionId + getSuffix();
    } else if (inspectionId.equals(XmlSuppressableInspectionTool.ALL)) {
      final int pos = originalText.indexOf(getPrefix());
      return originalText.substring(0, pos) + getPrefix() + inspectionId + getSuffix();
    }
    return originalText.replace(getSuffix(), ", " + inspectionId + getSuffix());
  }

  @NonNls
  protected String getPrefix() {
    return "<!--" +
        SUPPRESS_MARK +
        " ";
  }

  @NonNls
  protected String getSuffix() {
    return " -->\n";
  }
}
