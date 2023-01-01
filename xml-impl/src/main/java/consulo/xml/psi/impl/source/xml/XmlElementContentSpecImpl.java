/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.language.ast.ASTNode;
import consulo.language.ast.ChildRoleBase;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;
import consulo.logging.Logger;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlElementContentGroup;
import consulo.xml.psi.xml.XmlElementContentSpec;
import consulo.xml.psi.xml.XmlElementType;

import javax.annotation.Nonnull;

/**
 * @author Mike
 */
public class XmlElementContentSpecImpl extends XmlElementImpl implements XmlElementContentSpec, XmlElementType {
  private static final Logger LOG = Logger.getInstance(XmlElementContentSpecImpl.class);

  public XmlElementContentSpecImpl() {
    super(XML_ELEMENT_CONTENT_SPEC);
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == XML_CONTENT_ANY) {
      return XmlChildRole.XML_CONTENT_ANY;
    }
    else if (i == XML_CONTENT_EMPTY) {
      return XmlChildRole.XML_CONTENT_EMPTY;
    }
    else if (i == XML_PCDATA) {
      return XmlChildRole.XML_PCDATA;
    }
    else {
      return ChildRoleBase.NONE;
    }
  }

  public boolean isEmpty() {
    return findElementByTokenType(XML_CONTENT_EMPTY) != null;
  }

  public boolean isAny() {
    return findElementByTokenType(XML_CONTENT_ANY) != null;
  }

  public boolean isMixed() {
    XmlElementContentGroup topGroup = getTopGroup();
    return topGroup != null && ((XmlElementImpl)topGroup).findElementByTokenType(XML_PCDATA) != null;
  }

  public boolean hasChildren() {
    return !(isEmpty() || isAny() || isMixed());
  }

  @Override
  public XmlElementContentGroup getTopGroup() {
    return (XmlElementContentGroup)findElementByTokenType(XML_ELEMENT_CONTENT_GROUP);
  }

  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this,XmlElementContentSpec.class);
  }

  public void accept(@Nonnull final PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor)visitor).visitXmlElement(this);
    }
    else {
      visitor.visitElement(this);
    }
  }
}
