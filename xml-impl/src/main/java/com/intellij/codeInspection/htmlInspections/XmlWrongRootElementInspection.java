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

package com.intellij.codeInspection.htmlInspections;

import javax.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import com.intellij.codeInspection.*;
import consulo.application.Result;
import com.intellij.psi.html.HtmlTag;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.xml.XmlBundle;
import com.intellij.xml.util.XmlUtil;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

/**
 * @author spleaner
 */
public class XmlWrongRootElementInspection extends HtmlLocalInspectionTool {

  @Override
  @Nls
  @Nonnull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.XML_INSPECTIONS;
  }

  @Override
  @Nls
  @Nonnull
  public String getDisplayName() {
    return XmlBundle.message("xml.inspection.wrong.root.element");
  }

  @Override
  @NonNls
  @Nonnull
  public String getShortName() {
    return "XmlWrongRootElement";
  }

  @Override
  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Override
  protected void checkTag(@Nonnull final XmlTag tag, @Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
    if (!(tag.getParent() instanceof XmlTag)) {
      final PsiFile psiFile = tag.getContainingFile();
      if (!(psiFile instanceof XmlFile)) {
        return;
      }

      XmlFile xmlFile = (XmlFile) psiFile;

      final XmlDocument document = xmlFile.getDocument();
      if (document == null) {
        return;
      }

      XmlProlog prolog = document.getProlog();
      if (prolog == null || XmlHighlightVisitor.skipValidation(prolog)) {
        return;
      }

      final XmlDoctype doctype = prolog.getDoctype();

      if (doctype == null) {
        return;
      }

      XmlElement nameElement = doctype.getNameElement();

      if (nameElement == null) {
        return;
      }

      String name = tag.getName();
      String text = nameElement.getText();
      if (tag instanceof HtmlTag) {
        name = name.toLowerCase();
        text = text.toLowerCase();
      }

      if (!name.equals(text)) {
        name = XmlUtil.findLocalNameByQualifiedName(name);

        if (!name.equals(text)) {
          if (tag instanceof HtmlTag) {
            return; // it is legal to have html / head / body omitted
          }
          final LocalQuickFix localQuickFix = new MyLocalQuickFix(doctype.getNameElement().getText());

          holder.registerProblem(XmlChildRole.START_TAG_NAME_FINDER.findChild(tag.getNode()).getPsi(),
            XmlErrorMessages.message("wrong.root.element"),
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, localQuickFix
          );

          final ASTNode astNode = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(tag.getNode());
          if (astNode != null) {
            holder.registerProblem(astNode.getPsi(),
              XmlErrorMessages.message("wrong.root.element"),
              ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, localQuickFix
            );
          }
        }
      }
    }
  }

  private static class MyLocalQuickFix implements LocalQuickFix {
    private final String myText;

    public MyLocalQuickFix(String text) {
      myText = text;
    }

    @Override
    @Nonnull
    public String getName() {
      return XmlBundle.message("change.root.element.to", myText);
    }

    @Override
    @Nonnull
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
      final XmlTag myTag = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), XmlTag.class);

      if (!FileModificationService.getInstance().prepareFileForWrite(myTag.getContainingFile())) {
        return;
      }

      new WriteCommandAction(project) {
        @Override
        protected void run(final Result result) throws Throwable {
          myTag.setName(myText);
        }
      }.execute();
    }
  }
}
