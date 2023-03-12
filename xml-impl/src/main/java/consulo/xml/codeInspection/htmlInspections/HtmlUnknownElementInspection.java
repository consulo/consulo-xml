/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.xml.codeInspection.htmlInspections;

import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlChildRole;

import javax.annotation.Nonnull;

public abstract class HtmlUnknownElementInspection extends HtmlLocalInspectionTool {
  protected static void registerProblemOnAttributeName(@Nonnull XmlAttribute attribute,
                                                       String message,
                                                       @Nonnull ProblemsHolder holder,
                                                       LocalQuickFix... quickfixes) {
    final ASTNode node = attribute.getNode();
    assert node != null;
    final ASTNode nameNode = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(node);
    if (nameNode != null) {
      final PsiElement nameElement = nameNode.getPsi();
      if (nameElement.getTextLength() > 0) {
        holder.registerProblem(nameElement, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, quickfixes);
      }
    }
  }

  protected abstract LocalizeValue getCheckboxTitle();

  @Nonnull
  @Override
  public InspectionToolState<?> createStateProvider() {
    return new HtmlEntitiesInspectionState(getCheckboxTitle());
  }
}
