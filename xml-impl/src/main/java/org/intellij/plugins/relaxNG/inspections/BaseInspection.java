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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.ide.highlighter.XmlFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.plugins.relaxNG.compact.psi.*;

import java.util.function.Function;

/**
 * @author sweinreuter
 * @since 2007-11-25
 */
public abstract class BaseInspection extends XmlSuppressableInspectionTool {
    @Nonnull
    @Override
    public final LocalizeValue getGroupDisplayName() {
        return getRngGroupDisplayName();
    }

    public static LocalizeValue getRngGroupDisplayName() {
        return LocalizeValue.localizeTODO("RELAX NG");
    }

    @Override
    @RequiredReadAction
    @SuppressWarnings({"SSBasedInspection"})
    public boolean isSuppressedFor(@Nonnull PsiElement element) {
        if (element.getContainingFile() instanceof RncFile) {
            RncDefine define = PsiTreeUtil.getParentOfType(element, RncDefine.class, false);
            if (define != null && isSuppressedAt(define)) {
                return true;
            }

            RncGrammar grammar = PsiTreeUtil.getParentOfType(define, RncGrammar.class);
            return grammar != null && isSuppressedAt(grammar);
        }
        else {
            return super.isSuppressedFor(element);
        }
    }

    @RequiredReadAction
    @SuppressWarnings({"SSBasedInspection"})
    private boolean isSuppressedAt(RncElement location) {
        PsiElement prev = location.getPrevSibling();
        while (prev instanceof PsiWhiteSpace || prev instanceof PsiComment) {
            if (prev instanceof PsiComment prevComment) {
                String text = prevComment.getText();
                if (text.matches("\n*#\\s*suppress\\s.+") && (text.contains(getID()) || "ALL".equals(text))) {
                    return true;
                }
            }
            prev = prev.getPrevSibling();
        }
        return false;
    }

    @Nonnull
    @Override
    public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
        if (element.getContainingFile() instanceof RncFile) {
            return ArrayUtil.mergeArrays(
                new SuppressQuickFix[]{
                    new SuppressAction("Define") {
                        @Override
                        protected PsiElement getTarget(PsiElement element) {
                            return PsiTreeUtil.getParentOfType(element, RncDefine.class, false);
                        }
                    },
                    new SuppressAction("Grammar") {
                        @Override
                        @RequiredReadAction
                        protected PsiElement getTarget(PsiElement element) {
                            RncDefine define = PsiTreeUtil.getParentOfType(element, RncDefine.class, false);
                            RncGrammar target = define != null ? PsiTreeUtil.getParentOfType(define, RncGrammar.class, false) : null;
                            return target != null && target.getText().startsWith("grammar ") ? target : null;
                        }
                    }
                },
                getXmlOnlySuppressions(element)
            );
        }
        else {
            return super.getBatchSuppressActions(element);
        }
    }

    private SuppressQuickFix[] getXmlOnlySuppressions(PsiElement element) {
        return ContainerUtil.map(
            super.getBatchSuppressActions(element),
            action -> new SuppressQuickFix() {
                @Nonnull
                @Override
                public LocalizeValue getName() {
                    return action.getName();
                }

                @Override
                @RequiredReadAction
                public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
                    return context.isValid();
                }

                @Override
                @RequiredUIAccess
                public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
                    PsiElement element1 = descriptor.getPsiElement();
                    PsiFile file = element1 == null ? null : element1.getContainingFile();
                    if (file == null || file.getFileType() != XmlFileType.INSTANCE) {
                        return;
                    }
                    action.applyFix(project, descriptor);
                }
            },
            SuppressQuickFix.EMPTY_ARRAY
        );
    }

    @RequiredWriteAction
    private void suppress(PsiFile file, @Nonnull PsiElement location) {
        suppress(file, location, "#suppress " + getID(), text -> text + ", " + getID());
    }

    @RequiredWriteAction
    @SuppressWarnings({"SSBasedInspection"})
    private static void suppress(PsiFile file, @Nonnull PsiElement location, String suppressComment, Function<String, String> replace) {
        Project project = file.getProject();
        VirtualFile vfile = file.getVirtualFile();
        if (vfile == null || ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(vfile).hasReadonlyFiles()) {
            return;
        }

        Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
        assert doc != null;

        PsiElement leaf = location.getPrevSibling();

        while (leaf instanceof PsiWhiteSpace whiteSpace) {
            leaf = whiteSpace.getPrevSibling();
        }

        while (leaf instanceof PsiComment || leaf instanceof PsiWhiteSpace) {
            String text = leaf.getText();
            if (text.matches("\n*#\\s*suppress\\s.+")) {
                TextRange textRange = leaf.getTextRange();
                doc.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), replace.apply(text));
                return;
            }
            leaf = leaf.getPrevSibling();
        }

        int offset = location.getTextRange().getStartOffset();
        doc.insertString(offset, suppressComment + "\n");
        CodeStyleManager.getInstance(project).adjustLineIndent(doc, offset + suppressComment.length());
//        UndoManager.getInstance(file.getProject()).markDocumentForUndo(file);
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
        public LocalizeValue getName() {
            return LocalizeValue.localizeTODO("Suppress for " + myLocation);
        }

        @Override
        @RequiredReadAction
        public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
            return context.isValid();
        }

        @Override
        @RequiredWriteAction
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiElement target = getTarget(element);
            if (target == null) {
                return;
            }
            suppress(element.getContainingFile(), target);
        }

        protected abstract PsiElement getTarget(PsiElement element);
    }
}
