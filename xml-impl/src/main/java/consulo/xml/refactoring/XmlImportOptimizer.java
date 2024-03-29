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
import consulo.xml.codeInsight.daemon.impl.analysis.XmlUnusedNamespaceInspection;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
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
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Condition;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 *         Date: 11/7/11
 */
@ExtensionImpl(id = "xml")
public class XmlImportOptimizer implements ImportOptimizer {
  
  private final XmlUnusedNamespaceInspection myInspection = new XmlUnusedNamespaceInspection();
  private final Condition<ProblemDescriptor> myCondition = new Condition<ProblemDescriptor>() {
    @Override
    public boolean value(ProblemDescriptor descriptor) {
      PsiElement element = descriptor.getPsiElement();
      PsiElement parent = element.getParent();
      return parent != null && !myInspection.isSuppressedFor(parent);
    }
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
        ProblemsHolder holder = new ProblemsHolder(InspectionManager.getInstance(project), xmlFile, false);
        final XmlElementVisitor visitor = (XmlElementVisitor)myInspection.buildVisitor(holder, false);
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
        List<ProblemDescriptor> list = ContainerUtil.filter(results, myCondition);

        Map<XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix, ProblemDescriptor> fixes = new LinkedHashMap<XmlUnusedNamespaceInspection.RemoveNamespaceDeclarationFix, ProblemDescriptor>();
        for (ProblemDescriptor result : list) {
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
