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
package consulo.xml.ide.actions;

import com.intellij.xml.XmlBundle;
import consulo.application.dumb.DumbAware;
import consulo.fileTemplate.FileTemplateManager;
import consulo.ide.action.CreateFileFromTemplateAction;
import consulo.ide.action.CreateFileFromTemplateDialog;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XHtmlFileType;

/**
 * @author Eugene.Kudelevsky
 */
public class CreateHtmlFileAction extends CreateFileFromTemplateAction implements DumbAware {

  private static final String DEFAULT_HTML_TEMPLATE_PROPERTY = "DefaultHtmlFileTemplate";

  public CreateHtmlFileAction() {
    super(XmlBundle.message("new.html.file.action"), XmlBundle.message("new.html.file.action.description"), HtmlFileType.INSTANCE.getIcon());
  }

  @Override
  protected String getDefaultTemplateProperty() {
    return DEFAULT_HTML_TEMPLATE_PROPERTY;
  }

  @Override
  protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder
      .setTitle(XmlBundle.message("new.html.file.action"))
      .addKind("HTML file", HtmlFileType.INSTANCE.getIcon(), FileTemplateManager.INTERNAL_HTML5_TEMPLATE_NAME)
      .addKind("HTML4 file", HtmlFileType.INSTANCE.getIcon(), FileTemplateManager.INTERNAL_HTML_TEMPLATE_NAME)
      .addKind("XHTML file", XHtmlFileType.INSTANCE.getIcon(), FileTemplateManager.INTERNAL_XHTML_TEMPLATE_NAME);
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return XmlBundle.message("new.html.file.action");
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CreateHtmlFileAction;
  }
}
