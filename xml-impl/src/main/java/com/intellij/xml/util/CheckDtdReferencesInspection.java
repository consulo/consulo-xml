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
import consulo.application.progress.ProgressManager;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Maxim Mossienko
 */
@ExtensionImpl
public class CheckDtdReferencesInspection extends XmlSuppressableInspectionTool {
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            private Map<PsiFile, Boolean> myDoctypeMap = new HashMap<>();

            @Override
            @RequiredReadAction
            public void visitXmlElement(XmlElement element) {
                if (isHtml5Doctype(element)) {
                    return;
                }

                if (element instanceof XmlElementContentSpec || element instanceof XmlEntityRef) {
                    doCheckRefs(element, holder);
                }
            }

            private boolean isHtml5Doctype(XmlElement element) {
                if (HtmlUtil.isHtml5Context(element)) {
                    return true;
                }

                PsiFile file = element.getContainingFile();
                if (file instanceof XmlFile) {
                    if (!myDoctypeMap.containsKey(file)) {
                        myDoctypeMap.put(file, computeHtml5Doctype((XmlFile) file));
                    }
                    return myDoctypeMap.get(file);
                }
                return false;
            }

            private Boolean computeHtml5Doctype(XmlFile file) {
                //Search for doctypes from providers
                XmlDoctype doctype = file.getApplication().getExtensionPoint(HtmlDoctypeProvider.class)
                    .computeSafeIfAny(provider -> provider.getDoctype(file));
                return doctype != null && HtmlUtil.isHtml5Doctype(doctype);
            }
        };
    }

    @RequiredReadAction
    private static void doCheckRefs(XmlElement element, ProblemsHolder holder) {
        for (PsiReference ref : element.getReferences()) {
            ProgressManager.checkCanceled();
            if (XmlHighlightVisitor.hasBadResolve(ref, true)) {
                if (ref.getElement() instanceof XmlElementContentSpec) {
                    String image = ref.getCanonicalText();
                    if (image.equals("-") || image.equals("O")) {
                        continue;
                    }
                }
                holder.registerProblem(ref);
            }
        }
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlInspectionGroupNames.XML_INSPECTIONS;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.xmlInspectionsCheckDtdReferences();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "CheckDtdRefs";
    }
}
