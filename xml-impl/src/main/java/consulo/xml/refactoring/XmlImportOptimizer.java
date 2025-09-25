/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.xml.refactoring;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.QuickFix;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlUnusedNamespaceInspection;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 * @since 2011-07-11
 */
@ExtensionImpl(id = "xml")
public class XmlImportOptimizer implements ImportOptimizer {
  
  private final XmlUnusedNamespaceInspection myInspection = new XmlUnusedNamespaceInspection();

  private final Predicate<ProblemDescriptor> myCondition = descriptor -> {
    PsiElement element = descriptor.getPsiElement();
    PsiElement parent = element.getParent();
    return parent != null && !myInspection.isSuppressedFor(parent);
  };

  @Override
  public boolean supports(PsiFile file) {
    return file instanceof XmlFile;
  }

  @Nonnull
  @Override
  public Runnable processFile(final PsiFile file) {
    return new Runnable() {
      @Override
      public void run() {
        XmlFile xmlFile = (XmlFile)file;
        Project project = xmlFile.getProject();
        InspectionManager manager = InspectionManager.getInstance(project);
        ProblemsHolder holder = manager.createProblemsHolder(file, false);
        XmlElementVisitor visitor = (XmlElementVisitor)myInspection.buildVisitor(holder, false);
        new PsiRecursiveElementVisitor() {
          @Override
          public void visitElement(PsiElement element) {
            if (element instanceof XmlAttribute) {
              visitor.visitXmlAttribute((XmlAttribute)element);
            }
            else {
              super.visitElement(element);
            }
          }
        }.visitFile(xmlFile);
        ProblemDescriptor[] results = holder.getResultsArray();
        ArrayUtil.reverseArray(results);

        Map<XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix, ProblemDescriptor> fixes = new LinkedHashMap<>();
        for (ProblemDescriptor result : results) {
          if (!myCondition.test(result)) {
            continue;
          }

          for (QuickFix fix : result.getFixes()) {
            if (fix instanceof XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix) {
              fixes.put((XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix)fix, result);
            }
          }
        }

        SmartPsiElementPointer<XmlTag> pointer = null;
        for (Map.Entry<XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix, ProblemDescriptor> fix : fixes.entrySet()) {
          pointer = fix.getKey().doFix(project, fix.getValue(), false);
        }
        if (pointer != null) {
          XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix.reformatStartTag(project, pointer);
        }
      }
    };
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }
}
