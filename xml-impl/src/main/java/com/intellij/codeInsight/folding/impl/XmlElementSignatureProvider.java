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
package com.intellij.codeInsight.folding.impl;

import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.ide.impl.idea.codeInsight.folding.impl.AbstractElementSignatureProvider;
import consulo.logging.Logger;

/**
 * @author yole
 */
public class XmlElementSignatureProvider extends AbstractElementSignatureProvider {
  private static final Logger LOG = Logger.getInstance(XmlElementSignatureProvider.class);

  public String getSignature(@Nonnull PsiElement element) {
    if (element instanceof XmlTag) {
      XmlTag tag = (XmlTag)element;
      PsiElement parent = tag.getParent();

      StringBuilder buffer = new StringBuilder();
      buffer.append("tag").append(ELEMENT_TOKENS_SEPARATOR);
      String name = tag.getName();
      buffer.append(name.length() == 0 ? "<unnamed>" : name);

      buffer.append(ELEMENT_TOKENS_SEPARATOR);
      buffer.append(getChildIndex(tag, parent, name, XmlTag.class));

      if (parent instanceof XmlTag) {
        String parentSignature = getSignature(parent);
        buffer.append(";");
        buffer.append(parentSignature);
      }

      return buffer.toString();
    }
    return null;
  }

  @Override
  protected PsiElement restoreBySignatureTokens(@Nonnull PsiFile file,
                                                @Nonnull PsiElement parent,
                                                @Nonnull String type,
                                                @Nonnull StringTokenizer tokenizer,
                                                @Nullable StringBuilder processingInfoStorage)
  {
    if (type.equals("tag")) {
      String name = tokenizer.nextToken();

      if (parent instanceof XmlFile) {
        parent = ((XmlFile)parent).getDocument();
        if (parent == null) {
          return null;
        }
      }

      try {
        int index = Integer.parseInt(tokenizer.nextToken());

        return restoreElementInternal(parent, name, index, XmlTag.class);
      }
      catch (NumberFormatException e) {
        LOG.error(e);
        return null;
      }
    }
    return null;
  }
}
