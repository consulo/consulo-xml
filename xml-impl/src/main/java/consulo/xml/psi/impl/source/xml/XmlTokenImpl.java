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
package consulo.xml.psi.impl.source.xml;

import consulo.language.ast.IElementType;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.EditSourceUtil;
import consulo.navigation.Navigatable;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.tree.xml.IDTDElementType;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;

/**
 * @author ik
 */
public class XmlTokenImpl extends LeafPsiElement implements XmlToken, Navigatable {
  public XmlTokenImpl(IElementType type, CharSequence text) {
    super(type, text);
  }

  public boolean processElements(PsiElementProcessor processor, PsiElement place) {
    return false;
  }

  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor)visitor).visitXmlToken(this);
    }
    else {
      visitor.visitElement(this);
    }
  }

  public String toString() {
    if(getTokenType() instanceof IDTDElementType){
      return "DTDToken:" + getTokenType().toString();
    }
    return "XmlToken:" + getTokenType().toString();
  }

// Implementation specific

  public IElementType getTokenType() {
    return getElementType();
  }

  @Nonnull
  public PsiReference[] getReferences() {
    final IElementType elementType = getElementType();

    if (elementType == XmlTokenType.XML_DATA_CHARACTERS ||
        elementType == XmlTokenType.XML_CHAR_ENTITY_REF) {
      return ReferenceProvidersRegistry.getReferencesFromProviders(this, XmlToken.class);
    } else if (elementType == XmlTokenType.XML_NAME && getParent() instanceof PsiErrorElement) {
      final PsiElement element = getPrevSibling();
      
      if (element instanceof XmlToken && ((XmlToken)element).getTokenType() == XmlTokenType.XML_END_TAG_START) {
        return new PsiReference[] {TagNameReference.createTagNameReference(this, getNode(), false)};
      }
    }

    return super.getReferences();
  }

  public void navigate(boolean requestFocus) {
    EditSourceUtil.getDescriptor(this).navigate(requestFocus);
  }

  public boolean canNavigate() {
    return getTokenType() == XmlTokenType.XML_NAME && EditSourceUtil.canNavigate(this);
  }

  public boolean canNavigateToSource() {
    return canNavigate();
  }
}
