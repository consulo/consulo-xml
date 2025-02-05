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
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
public class XmlDeclareIdInCommentAction implements LocalQuickFix {
    private static final Logger LOG = Logger.getInstance(XmlDeclareIdInCommentAction.class);

    private final String myId;

    public XmlDeclareIdInCommentAction(@Nonnull final String id) {
        myId = id;
    }

    @Nonnull
    public String getName() {
        return XmlErrorLocalize.declareIdInCommentQuickfix().get();
    }

    @Nonnull
    public String getFamilyName() {
        return getName();
    }

    @Nullable
    public static String getImplicitlyDeclaredId(@Nonnull final PsiComment comment) {
        final String text = getUncommentedText(comment);
        if (text == null) {
            return null;
        }

        if (text.startsWith("@declare id=\"")) {
            final String result = text.substring("@declare id=\"".length() - 1);
            return StringUtil.unquoteString(result);
        }

        return null;
    }

    @Nullable
    private static String getUncommentedText(@Nonnull final PsiComment comment) {
        final PsiFile psiFile = comment.getContainingFile();
        final Language language = psiFile.getViewProvider().getBaseLanguage();
        final Commenter commenter = Commenter.forLanguage(language);
        if (commenter != null) {
            String text = comment.getText();

            final String prefix = commenter.getBlockCommentPrefix();
            if (prefix != null && text.startsWith(prefix)) {
                text = text.substring(prefix.length());
                final String suffix = commenter.getBlockCommentSuffix();
                if (suffix != null && text.length() > suffix.length()) {
                    return text.substring(0, text.length() - suffix.length()).trim();
                }
            }
        }

        return null;
    }

    public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
        final PsiElement psiElement = descriptor.getPsiElement();
        final PsiFile psiFile = psiElement.getContainingFile();

        new WriteCommandAction(project, psiFile) {
            protected void run(final Result result) throws Throwable {
                final XmlTag tag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
                if (tag == null) {
                    return;
                }

                final Language language = psiFile.getViewProvider().getBaseLanguage();
                final Commenter commenter = Commenter.forLanguage(language);
                if (commenter == null) {
                    return;
                }

                final PsiFile tempFile = PsiFileFactory.getInstance(project).createFileFromText(
                    "dummy",
                    language.getAssociatedFileType(),
                    commenter.getBlockCommentPrefix() +
                        "@declare id=\"" + myId + "\"" +
                        commenter.getBlockCommentSuffix() + "\n"
                );

                final XmlTag parent = tag.getParentTag();
                if (parent != null && parent.isValid()) {
                    final XmlTag[] tags = parent.getSubTags();
                    if (tags.length > 0) {
                        final PsiFile psi = tempFile.getViewProvider().getPsi(language);
                        if (psi != null && psi.findElementAt(1) instanceof PsiComment comment) {
                            parent.getNode().addChild(comment.getNode(), tags[0].getNode());
                        }
                    }
                }
            }
        }.execute();
    }
}
