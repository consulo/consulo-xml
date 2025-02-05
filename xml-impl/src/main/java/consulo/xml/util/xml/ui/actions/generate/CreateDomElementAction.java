/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.xml.util.xml.ui.actions.generate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.language.editor.template.Expression;
import consulo.codeEditor.Editor;
import consulo.language.psi.ElementManipulators;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.GenericDomValue;
import consulo.language.editor.action.SimpleCodeInsightAction;
import consulo.language.editor.template.TemplateBuilder;

/**
 * @author Dmitry Avdeev
 */
public abstract class CreateDomElementAction<T extends DomElement> extends SimpleCodeInsightAction {

  private final Class<T> myContextClass;

  public CreateDomElementAction(Class<T> contextClass) {
    myContextClass = contextClass;
  }

  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    DomElement element = createElement(getContextElement(editor), editor, file, project);
  }

  @Nullable
  protected abstract DomElement createElement(T context, Editor editor, PsiFile file, Project project);

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return getContextElement(editor) != null;
  }

  @Nullable
  protected T getContextElement(Editor editor) {
    return DomUtil.getContextElement(editor, myContextClass);
  }

  public static void replaceElementValue(TemplateBuilder builder, GenericDomValue element, Expression expression) {
    element.setStringValue("");
    XmlElement xmlElement = element.getXmlElement();
    builder.replaceElement(xmlElement, ElementManipulators.getValueTextRange(xmlElement), expression);
  }
}
