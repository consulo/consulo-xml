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

import com.intellij.xml.XmlBundle;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Maxim Mossienko
 */
@ExtensionImpl
public class CheckDtdReferencesInspection extends XmlSuppressableInspectionTool {
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nonnull
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {

      private Map<PsiFile, Boolean> myDoctypeMap = new HashMap<PsiFile, Boolean>();

      @Override
      public void visitXmlElement(final XmlElement element) {
        if (isHtml5Doctype(element)) {
          return;
        }

        if (element instanceof XmlElementContentSpec ||
            element instanceof XmlEntityRef
          ) {
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
            myDoctypeMap.put(file, computeHtml5Doctype((XmlFile)file));
          }
          return myDoctypeMap.get(file);
        }
        return false;
      }

      private Boolean computeHtml5Doctype(XmlFile file) {
        XmlDoctype doctype = null;
        //Search for doctypes from providers
        for (HtmlDoctypeProvider provider : HtmlDoctypeProvider.EP_NAME.getExtensionList()) {
          doctype = provider.getDoctype(file);
          if (doctype != null) {
            break;
          }
        }

        if (doctype != null && HtmlUtil.isHtml5Doctype(doctype)) {
          return true;
        }

        return false;
      }
    };
  }

  private static void doCheckRefs(final XmlElement element, final ProblemsHolder holder) {
    for (PsiReference ref : element.getReferences()) {
      ProgressManager.checkCanceled();
      if (XmlHighlightVisitor.hasBadResolve(ref, true)) {
        if (ref.getElement() instanceof XmlElementContentSpec) {
          final String image = ref.getCanonicalText();
          if (image.equals("-") || image.equals("O")) continue;
        }
        holder.registerProblem(ref);
      }
    }
  }

  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Nonnull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.XML_INSPECTIONS;
  }

  @Nonnull
  public String getDisplayName() {
    return XmlBundle.message("xml.inspections.check.dtd.references");
  }

  @Nonnull
  @NonNls
  public String getShortName() {
    return "CheckDtdRefs";
  }

}
