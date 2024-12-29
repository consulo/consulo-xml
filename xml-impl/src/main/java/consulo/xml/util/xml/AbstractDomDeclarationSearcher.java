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
package consulo.xml.util.xml;

import consulo.language.ast.IElementType;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.pom.PomDeclarationSearcher;
import consulo.language.pom.PomTarget;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author Eugene Zhuravlev
 *         Date: Jun 23, 2010
 */
public abstract class AbstractDomDeclarationSearcher extends PomDeclarationSearcher {

  public void findDeclarationsAt(@Nonnull PsiElement psiElement, int offsetInElement, Consumer<PomTarget> consumer) {
    if (!(psiElement instanceof XmlToken)) return;

    final IElementType tokenType = ((XmlToken)psiElement).getTokenType();

    InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(psiElement.getProject());
    final DomManager domManager = DomManager.getDomManager(psiElement.getProject());
    final DomElement nameElement;
    if (tokenType == XmlTokenType.XML_DATA_CHARACTERS && psiElement.getParent() instanceof XmlText && psiElement.getParent().getParent() instanceof XmlTag) {
      final XmlTag tag = (XmlTag)psiElement.getParent().getParent();
      for (XmlText text : tag.getValue().getTextElements()) {
        if (injectedLanguageManager.getInjectedPsiFiles(text) != null) {
          return;
        }
      }

      nameElement = domManager.getDomElement(tag);
    } else if (tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN && psiElement.getParent() instanceof XmlAttributeValue && psiElement.getParent().getParent() instanceof XmlAttribute) {
      final PsiElement attributeValue = psiElement.getParent();
      if (injectedLanguageManager.getInjectedPsiFiles(attributeValue) != null) {
        return;
      }
      nameElement = domManager.getDomElement((XmlAttribute)attributeValue.getParent());
    } else {
      return;
    }

    if (!(nameElement instanceof GenericDomValue)) {
      return;
    }

    DomElement parent = nameElement.getParent();
    if (parent == null) {
      return;
    }

    final DomTarget target = createDomTarget(parent, nameElement);
    if (target != null) {
      consumer.accept(target);
    }
  }

  @Nullable
  protected abstract DomTarget createDomTarget(DomElement parent, DomElement nameElement);
}
