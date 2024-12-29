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

package consulo.xml.util.xml.ui.actions.generate;

import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * User: Sergey.Vasiliev
 */
public class GenerateDomElementAction extends CodeInsightAction {

  protected final GenerateDomElementProvider myProvider;

  public GenerateDomElementAction(@Nonnull final GenerateDomElementProvider generateProvider, @Nullable Image icon) {
    getTemplatePresentation().setDescription(generateProvider.getDescription());
    getTemplatePresentation().setText(generateProvider.getDescription());
    getTemplatePresentation().setIcon(icon);

    myProvider = generateProvider;
    
  }

  public GenerateDomElementAction(final GenerateDomElementProvider generateProvider) {
      this(generateProvider, null);
  }

  @Nonnull
  protected CodeInsightActionHandler getHandler() {
    return new CodeInsightActionHandler() {
      public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
        final Runnable runnable = new Runnable() {
          public void run() {
            final DomElement element = myProvider.generate(project, editor, file);
            myProvider.navigate(element);
          }
        };
        
        if (GenerateDomElementAction.this.startInWriteAction()) {
          new WriteCommandAction(project, file) {
            protected void run(final Result result) throws Throwable {
              runnable.run();
            }
          }.execute();
        }
        else {
          runnable.run();
        }
      }

      public boolean startInWriteAction() {
        return false;
      }
    };
  }

  protected boolean startInWriteAction() {
    return true;
  }

  protected boolean isValidForFile(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
    final DomElement element = DomUtil.getContextElement(editor);
    return element != null && myProvider.isAvailableForElement(element);
  }

  public GenerateDomElementProvider getProvider() {
    return myProvider;
  }
}
