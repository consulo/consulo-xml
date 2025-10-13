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
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlDuplicatedIdInspection extends XmlSuppressableInspectionTool implements UnfairLocalInspectionTool {
    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlLocalize.xmlInspectionsGroupName();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.xmlInspectionsDuplicateId();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitXmlAttributeValue(XmlAttributeValue value) {
                if (value.getTextRange().isEmpty()) {
                    return;
                }
                PsiFile file = value.getContainingFile();
                if (!(file instanceof XmlFile)) {
                    return;
                }
                PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
                if (baseFile != file && !(baseFile instanceof XmlFile)) {
                    return;
                }
                XmlRefCountHolder refHolder = XmlRefCountHolder.getRefCountHolder(value);
                if (refHolder == null) {
                    return;
                }

                PsiElement parent = value.getParent();
                if (!(parent instanceof XmlAttribute)) {
                    return;
                }

                XmlTag tag = (XmlTag) parent.getParent();
                if (tag == null) {
                    return;
                }

                checkValue(value, (XmlFile) file, refHolder, tag, holder);
            }
        };
    }

    protected void checkValue(XmlAttributeValue value, XmlFile file, XmlRefCountHolder refHolder, XmlTag tag, ProblemsHolder holder) {
        if (refHolder.isValidatable(tag.getParent()) && refHolder.isDuplicateIdAttributeValue(value)) {
            holder.newProblem(XmlErrorLocalize.duplicateIdReference())
                .range(value, ElementManipulators.getValueTextRange(value))
                .highlightType(ProblemHighlightType.GENERIC_ERROR)
                .create();
        }
    }
}
