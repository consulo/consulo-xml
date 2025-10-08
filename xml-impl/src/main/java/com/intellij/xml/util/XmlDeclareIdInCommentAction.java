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
package com.intellij.xml.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Result;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
public class XmlDeclareIdInCommentAction implements LocalQuickFix {
    private final String myId;

    public XmlDeclareIdInCommentAction(@Nonnull String id) {
        myId = id;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return XmlErrorLocalize.declareIdInCommentQuickfix();
    }

    @Nullable
    @RequiredReadAction
    public static String getImplicitlyDeclaredId(@Nonnull PsiComment comment) {
        String text = getUncommentedText(comment);
        if (text == null) {
            return null;
        }

        if (text.startsWith("@declare id=\"")) {
            String result = text.substring("@declare id=\"".length() - 1);
            return StringUtil.unquoteString(result);
        }

        return null;
    }

    @Nullable
    @RequiredReadAction
    private static String getUncommentedText(@Nonnull PsiComment comment) {
        PsiFile psiFile = comment.getContainingFile();
        Language language = psiFile.getViewProvider().getBaseLanguage();
        Commenter commenter = Commenter.forLanguage(language);
        if (commenter != null) {
            String text = comment.getText();

            String prefix = commenter.getBlockCommentPrefix();
            if (prefix != null && text.startsWith(prefix)) {
                text = text.substring(prefix.length());
                String suffix = commenter.getBlockCommentSuffix();
                if (suffix != null && text.length() > suffix.length()) {
                    return text.substring(0, text.length() - suffix.length()).trim();
                }
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull final Project project, @Nonnull ProblemDescriptor descriptor) {
        final PsiElement psiElement = descriptor.getPsiElement();
        final PsiFile psiFile = psiElement.getContainingFile();

        new WriteCommandAction(project, psiFile) {
            @Override
            protected void run(Result result) throws Throwable {
                XmlTag tag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
                if (tag == null) {
                    return;
                }

                Language language = psiFile.getViewProvider().getBaseLanguage();
                Commenter commenter = Commenter.forLanguage(language);
                if (commenter == null) {
                    return;
                }

                PsiFile tempFile = PsiFileFactory.getInstance(project).createFileFromText(
                    "dummy",
                    language.getAssociatedFileType(),
                    commenter.getBlockCommentPrefix() +
                        "@declare id=\"" + myId + "\"" +
                        commenter.getBlockCommentSuffix() + "\n"
                );

                XmlTag parent = tag.getParentTag();
                if (parent != null && parent.isValid()) {
                    XmlTag[] tags = parent.getSubTags();
                    if (tags.length > 0) {
                        PsiFile psi = tempFile.getViewProvider().getPsi(language);
                        if (psi != null && psi.findElementAt(1) instanceof PsiComment comment) {
                            parent.getNode().addChild(comment.getNode(), tags[0].getNode());
                        }
                    }
                }
            }
        }.execute();
    }
}
