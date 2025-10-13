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

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionPoint;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.file.MultiplePsiFilesPerDocumentFileViewProvider;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlInvalidIdInspection extends XmlDuplicatedIdInspection implements UnfairLocalInspectionTool {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.xmlInspectionsInvalidId();
    }

    @Override
    protected void checkValue(XmlAttributeValue value, XmlFile file, XmlRefCountHolder refHolder, XmlTag tag, ProblemsHolder holder) {
        String idRef = XmlHighlightVisitor.getUnquotedValue(value, tag);

        if (tag instanceof HtmlTag) {
            idRef = idRef.toLowerCase();
        }

        if (XmlUtil.isSimpleValue(idRef, value) && refHolder.isIdReferenceValue(value)) {
            boolean hasIdDeclaration = refHolder.hasIdDeclaration(idRef);
            if (!hasIdDeclaration && tag instanceof HtmlTag) {
                hasIdDeclaration = refHolder.hasIdDeclaration(value.getValue());
            }

            if (!hasIdDeclaration) {
                ExtensionPoint<XmlIdContributor> extensionPoint = file.getApplication().getExtensionPoint(XmlIdContributor.class);
                if (extensionPoint.anyMatchSafe(contributor -> contributor.suppressExistingIdValidation(file))) {
                    return;
                }

                FileViewProvider viewProvider = tag.getContainingFile().getViewProvider();
                if (viewProvider instanceof MultiplePsiFilesPerDocumentFileViewProvider) {
                    holder.newProblem(XmlErrorLocalize.invalidIdReference())
                        .range(value)
                        .withFix(new XmlDeclareIdInCommentAction(idRef))
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create();
                }
                else {
                    holder.newProblem(XmlErrorLocalize.invalidIdReference())
                        .range(value)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create();
                }
            }
        }
    }
}
