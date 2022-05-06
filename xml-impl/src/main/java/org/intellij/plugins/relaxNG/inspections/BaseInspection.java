/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.inspections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElement;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 25.11.2007
 */
public abstract class BaseInspection extends XmlSuppressableInspectionTool {
  @Override
  @Nls
  @Nonnull
  public final String getGroupDisplayName() {
    return getRngGroupDisplayName();
  }

  public static String getRngGroupDisplayName() {
    return "RELAX NG";
  }

  @SuppressWarnings({ "SSBasedInspection" })
  @Override
  public boolean isSuppressedFor(@Nonnull PsiElement element) {
    if (element.getContainingFile() instanceof RncFile) {
      final RncDefine define = PsiTreeUtil.getParentOfType(element, RncDefine.class, false);
      if (define != null) {
        if (isSuppressedAt(define)) return true;
      }

      final RncGrammar grammar = PsiTreeUtil.getParentOfType(define, RncGrammar.class);
      if (grammar != null) {
        if (isSuppressedAt(grammar)) return true;
      }

      return false;
    } else {
      return super.isSuppressedFor(element);
    }
  }

  @SuppressWarnings({ "SSBasedInspection" })
  private boolean isSuppressedAt(RncElement location) {
    PsiElement prev = location.getPrevSibling();
    while (prev instanceof PsiWhiteSpace || prev instanceof PsiComment) {
      if (prev instanceof PsiComment) {
        @NonNls String text = prev.getText();
        if (text.matches("\n*#\\s*suppress\\s.+") && (text.contains(getID()) || "ALL".equals(text))) return true;
      }
      prev = prev.getPrevSibling();
    }
    return false;
  }

  @Nonnull
  @Override
  public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
    if (element.getContainingFile() instanceof RncFile) {
      return ArrayUtil.mergeArrays(new SuppressQuickFix[] {
              new SuppressAction("Define") {
                @Override
                protected PsiElement getTarget(PsiElement element) {
                  return PsiTreeUtil.getParentOfType(element, RncDefine.class, false);
                }
              },
              new SuppressAction("Grammar") {
                @Override
                protected PsiElement getTarget(PsiElement element) {
                  final RncDefine define = PsiTreeUtil.getParentOfType(element, RncDefine.class, false);
                  RncGrammar target = define != null ? PsiTreeUtil.getParentOfType(define, RncGrammar.class, false) : null;
                  return target != null && target.getText().startsWith("grammar ") ? target : null;
                }
              }
      }, getXmlOnlySuppressions(element));
    }
    else {
      return super.getBatchSuppressActions(element);
    }
  }

  private SuppressQuickFix[] getXmlOnlySuppressions(PsiElement element) {
    return ContainerUtil.map(super.getBatchSuppressActions(element), action -> new SuppressQuickFix() {
      @Nonnull
      @Override
      public String getName() {
        return action.getName();
      }

      @Override
      public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
        return context.isValid();
      }

      @Override
      public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement element1 = descriptor.getPsiElement();
        PsiFile file = element1 == null ? null : element1.getContainingFile();
        if (file == null || file.getFileType() != XmlFileType.INSTANCE) return;
        action.applyFix(project, descriptor);
      }

      @Override
      @Nonnull
      public String getFamilyName() {
        return action.getFamilyName();
      }
    }, SuppressQuickFix.EMPTY_ARRAY);
  }

  private void suppress(PsiFile file, @Nonnull PsiElement location) {
    suppress(file, location, "#suppress " + getID(), text -> text + ", " + getID());
  }

  @SuppressWarnings({ "SSBasedInspection" })
  private static void suppress(PsiFile file, @Nonnull PsiElement location, String suppressComment, Function<String, String> replace) {
    final Project project = file.getProject();
    final VirtualFile vfile = file.getVirtualFile();
    if (vfile == null || ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(vfile).hasReadonlyFiles()) {
      return;
    }

    final Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
    assert doc != null;

    PsiElement leaf = location.getPrevSibling();

    while (leaf instanceof PsiWhiteSpace) leaf = leaf.getPrevSibling();

    while (leaf instanceof PsiComment || leaf instanceof PsiWhiteSpace) {
      @NonNls String text = leaf.getText();
      if (text.matches("\n*#\\s*suppress\\s.+")) {
        final TextRange textRange = leaf.getTextRange();
        doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), replace.fun(text));
        return;
      }
      leaf = leaf.getPrevSibling();
    }

    final int offset = location.getTextRange().getStartOffset();
    doc.insertString(offset, suppressComment + "\n");
    CodeStyleManager.getInstance(project).adjustLineIndent(doc, offset + suppressComment.length());
//    UndoManager.getInstance(file.getProject()).markDocumentForUndo(file);
  }

  @Override
  @Nonnull
  public abstract RncElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly);

  private abstract class SuppressAction implements SuppressQuickFix {
    private final String myLocation;

    public SuppressAction(String location) {
      myLocation = location;
    }

    @Nonnull
    @Override
    public String getName() {
      return "Suppress for " + myLocation;
    }

    @Override
    @Nonnull
    public String getFamilyName() {
      return getDisplayName();
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
      return context.isValid();
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      PsiElement target = getTarget(element);
      if (target == null) return;
      suppress(element.getContainingFile(), target);
    }

    protected abstract PsiElement getTarget(PsiElement element);
  }
}
