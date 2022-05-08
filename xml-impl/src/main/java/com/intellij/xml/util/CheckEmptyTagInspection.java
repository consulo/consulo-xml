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
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Maxim Mossienko
 */
public class CheckEmptyTagInspection extends XmlSuppressableInspectionTool {
  private static final Logger LOG = Logger.getInstance(CheckEmptyTagInspection.class);
  @NonNls private static final String SCRIPT_TAG_NAME = "script";
  private static final Set<String> ourTagsWithEmptyEndsNotAllowed = new HashSet<String>(Arrays.asList(SCRIPT_TAG_NAME, "div", "iframe"));

  public boolean isEnabledByDefault() {
    return true;
  }

  @Nonnull
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {
      @Override public void visitXmlTag(final XmlTag tag) {
        if (!isTagWithEmptyEndNotAllowed(tag)) {
          return;
        }
        final ASTNode child = XmlChildRole.EMPTY_TAG_END_FINDER.findChild(tag.getNode());

        if (child == null) {
          return;
        }

        final LocalQuickFix fix = new MyLocalQuickFix();

        holder.registerProblem(tag,
                               XmlBundle.message("html.inspections.check.empty.script.message"),
                               tag.getContainingFile().getContext() != null ?
                               ProblemHighlightType.INFORMATION:
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                               fix);
      }
    };
  }

  static boolean isTagWithEmptyEndNotAllowed(final XmlTag tag) {
    String tagName = tag.getName();
    if (tag instanceof HtmlTag) tagName = tagName.toLowerCase();

    Language language = tag.getLanguage();
    return ourTagsWithEmptyEndsNotAllowed.contains(tagName) && language != XMLLanguage.INSTANCE ||
           language == HTMLLanguage.INSTANCE && !HtmlUtil.isSingleHtmlTagL(tagName) && tagName.indexOf(':') == -1;
  }

  @Nonnull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.HTML_INSPECTIONS;
  }

  @Nonnull
  public String getDisplayName() {
    return XmlBundle.message("html.inspections.check.empty.tag");
  }

  @Nonnull
  @NonNls
  public String getShortName() {
    return "CheckEmptyScriptTag";
  }

  private static class MyLocalQuickFix implements LocalQuickFix {
    @Nonnull
    public String getName() {
      return XmlBundle.message("html.inspections.check.empty.script.tag.fix.message");
    }

    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      final XmlTag tag = (XmlTag)descriptor.getPsiElement();
      if (tag == null) return;
      final PsiFile psiFile = tag.getContainingFile();

      if (psiFile == null) return;
      ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(psiFile.getVirtualFile());

      try {
        XmlUtil.expandTag(tag);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    //to appear in "Apply Fix" statement when multiple Quick Fixes exist
    @Nonnull
    public String getFamilyName() {
      return getName();
    }
  }
}
