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
package consulo.xml.codeInspection;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class XmlQuickFixFactory {
  @Nonnull
  public static XmlQuickFixFactory getInstance() {
    return ServiceManager.getService(XmlQuickFixFactory.class);
  }

  @Nonnull
  public abstract LocalQuickFixAndIntentionActionOnPsiElement insertRequiredAttributeFix(@Nonnull XmlTag tag, @Nonnull String attrName, @Nonnull String... values);

  @Nonnull
  public abstract LocalQuickFix createNSDeclarationIntentionFix(@Nonnull final PsiElement element, @Nonnull String namespacePrefix, @Nullable final XmlToken token);

  @Nonnull
  public abstract LocalQuickFixAndIntentionActionOnPsiElement addAttributeValueFix(@Nonnull XmlAttribute attribute);
}
