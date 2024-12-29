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

/*
 * User: anna
 * Date: 01-Feb-2008
 */
package consulo.xml.codeInsight.hint;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.ImplementationTextSelectioner;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class XmlImplementationTextSelectioner implements ImplementationTextSelectioner {
  private static final Logger LOG = Logger.getInstance(XmlImplementationTextSelectioner.class);

  public int getTextStartOffset(@Nonnull final PsiElement parent) {
    return parent.getTextRange().getStartOffset();
  }

  public int getTextEndOffset(@Nonnull PsiElement element) {
    if (element instanceof XmlAttributeValue) {
      final XmlTag xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag.class);// for convenience
      if (xmlTag != null) return xmlTag.getTextRange().getEndOffset();
      LOG.assertTrue(false);
    }
    return element.getTextRange().getEndOffset();
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }
}