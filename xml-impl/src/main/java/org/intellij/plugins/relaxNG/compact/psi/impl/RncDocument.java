/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.compact.psi.impl;

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlProlog;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlNSDescriptor;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.meta.MetaDataService;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.resolve.PsiElementProcessor;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RncDocument extends RncElementImpl implements XmlDocument {
  public RncDocument(ASTNode node) {
    super(node);
  }

  @Override
  public XmlNSDescriptor getDefaultNSDescriptor(String namespace, boolean strict) {
    return null;
  }

  @Override
  public XmlProlog getProlog() {
    return null;
  }

  @Override
  @Nullable
  public XmlTag getRootTag() {
    return null;
  }

  @Override
  public XmlNSDescriptor getRootTagNSDescriptor() {
    return null;
  }

  @Override
  public boolean processElements(PsiElementProcessor processor, PsiElement place) {
    return false;
  }

  @Override
  @Nullable
  public PsiMetaData getMetaData() {
    return MetaDataService.getInstance().getMeta(this);
  }

  public boolean isMetaEnough() {
    return true;
  }

  public RncGrammar getGrammar() {
    return findChildByClass(RncGrammar.class);
  }

  @Override
  @Nonnull
  protected <T> T[] findChildrenByClass(Class<T> aClass) {
    return super.findChildrenByClass(aClass);
  }

  @Override
  public void accept(@Nonnull RncElementVisitor visitor) {
    visitor.visitElement(this);
  }
}
