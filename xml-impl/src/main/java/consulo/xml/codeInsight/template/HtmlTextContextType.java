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
package consulo.xml.codeInsight.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.context.BaseTemplateContextType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.xml.XmlComment;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlText;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class HtmlTextContextType extends BaseTemplateContextType {
  public HtmlTextContextType() {
    super("HTML_TEXT", CodeInsightLocalize.dialogEditTemplateCheckboxHtmlText(), HtmlContextType.class);
  }

  @Override
  public boolean isInContext(@Nonnull PsiFile file, int offset) {
    if (!HtmlContextType.isMyLanguage(file.getLanguage())) {
      return false;
    }
    PsiElement element = file.findElementAt(offset);
    return element == null || isInContext(element);
  }

  public static boolean isInContext(@Nonnull PsiElement element) {
    if (PsiTreeUtil.getParentOfType(element, XmlComment.class) != null) {
      return false;
    }
    if (PsiTreeUtil.getParentOfType(element, XmlText.class) != null) {
      return true;
    }
    if (element.getNode().getElementType() == XmlTokenType.XML_START_TAG_START) {
      return true;
    }
    PsiElement parent = element.getParent();
    if (parent instanceof PsiErrorElement) {
      parent = parent.getParent();
    }
    return parent instanceof XmlDocument;
  }
}
