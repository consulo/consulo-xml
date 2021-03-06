/*
 * Copyright 2013 Consulo.org
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
package com.intellij.lang.xml;

import static com.intellij.psi.xml.XmlTokenType.XML_REAL_WHITE_SPACE;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.impl.source.xml.XmlTokenImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.xml.IXmlLeafElementType;
import consulo.lang.LanguageVersion;
import consulo.psi.tree.ASTLeafFactory;

/**
 * @author VISTALL
 * @since 2:41/02.04.13
 */
public class XmlASTLeafFactory implements ASTLeafFactory
{

  static {
    PsiBuilderImpl.registerWhitespaceToken(XML_REAL_WHITE_SPACE);
  }

  @Nonnull
  @Override
  public LeafElement createLeaf(@Nonnull IElementType type, @Nonnull LanguageVersion languageVersion, @Nonnull CharSequence text) {
    if (type instanceof IXmlLeafElementType) {
      if (type == XML_REAL_WHITE_SPACE) {
        return new PsiWhiteSpaceImpl(text);
      }
      return new XmlTokenImpl(type, text);
    }

    return null;
  }

  @Override
  public boolean apply(@Nullable IElementType input) {
    return input instanceof IXmlLeafElementType;
  }
}
