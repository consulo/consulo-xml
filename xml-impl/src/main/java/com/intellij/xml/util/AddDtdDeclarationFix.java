/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

public class AddDtdDeclarationFix implements LocalQuickFix {
    @Nonnull
    private final Function<Object, LocalizeValue> myNameTemplate;
    @Nonnull
    private final String myElementDeclarationName;
    @Nonnull
    private final String myReference;

    @RequiredReadAction
    public AddDtdDeclarationFix(
        @Nonnull Function<Object, LocalizeValue> nameTemplate,
        @Nonnull String elementDeclarationName,
        @Nonnull PsiReference reference
    ) {
        myNameTemplate = nameTemplate;
        myElementDeclarationName = elementDeclarationName;
        myReference = reference.getCanonicalText();
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return myNameTemplate.apply(myReference);
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        PsiFile containingFile = element.getContainingFile();

        String prefixToInsert = "";
        String suffixToInsert = "";

        int UNDEFINED_OFFSET = -1;
        int anchorOffset = UNDEFINED_OFFSET;
        PsiElement anchor = PsiTreeUtil.getParentOfType(
            element,
            XmlElementDecl.class,
            XmlAttlistDecl.class,
            XmlEntityDecl.class,
            XmlConditionalSection.class
        );
        if (anchor != null) {
            anchorOffset = anchor.getTextRange().getStartOffset();
        }

        if (anchorOffset == UNDEFINED_OFFSET && containingFile.getLanguage() == XMLLanguage.INSTANCE) {
            XmlFile file = (XmlFile) containingFile;
            XmlProlog prolog = file.getDocument().getProlog();
            assert prolog != null;

            XmlDoctype doctype = prolog.getDoctype();
            XmlMarkupDecl markupDecl;

            if (doctype != null) {
                markupDecl = doctype.getMarkupDecl();
            }
            else {
                markupDecl = null;
            }

            if (doctype == null) {
                XmlTag rootTag = file.getDocument().getRootTag();
                prefixToInsert = "<!DOCTYPE " + (rootTag != null ? rootTag.getName() : "null");
                suffixToInsert = ">\n";
            }
            if (markupDecl == null) {
                prefixToInsert += " [\n";
                suffixToInsert = "]" + suffixToInsert;

                if (doctype != null) {
                    anchorOffset = doctype.getTextRange().getEndOffset() - 1; // just before last '>'
                }
                else {
                    anchorOffset = prolog.getTextRange().getEndOffset();
                }
            }
        }

        if (anchorOffset == UNDEFINED_OFFSET) {
            anchorOffset = element.getTextRange().getStartOffset();
        }

        OpenFileDescriptor openDescriptor =
            OpenFileDescriptorFactory.getInstance(project).builder(containingFile.getVirtualFile()).offset(anchorOffset).build();
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(openDescriptor, true);
        TemplateManager templateManager = TemplateManager.getInstance(project);
        Template t = templateManager.createTemplate("", "");

        if (!prefixToInsert.isEmpty()) {
            t.addTextSegment(prefixToInsert);
        }
        t.addTextSegment("<!" + myElementDeclarationName + " " + myReference + " ");
        t.addEndVariable();
        t.addTextSegment(">\n");
        if (!suffixToInsert.isEmpty()) {
            t.addTextSegment(suffixToInsert);
        }
        templateManager.startTemplate(editor, t);
    }
}
