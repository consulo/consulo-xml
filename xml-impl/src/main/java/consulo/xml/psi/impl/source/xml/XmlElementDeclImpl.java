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

import com.intellij.xml.util.XmlUtil;
import consulo.ide.impl.psi.tree.ChildRoleBase;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;
import consulo.language.psi.meta.MetaDataService;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.xml.psi.xml.*;

import javax.annotation.Nonnull;

/**
 * @author Mike
 */
public class XmlElementDeclImpl extends XmlElementImpl implements XmlElementDecl, XmlElementType {
  private static final Logger LOG = Logger.getInstance("#XmlElementDeclImpl");

  public XmlElementDeclImpl() {
    super(XML_ELEMENT_DECL);
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == XML_NAME) {
      return XmlChildRole.XML_NAME;
    } else if (i == XML_ELEMENT_CONTENT_SPEC) {
      return XmlChildRole.XML_ELEMENT_CONTENT_SPEC;
    } else {
      return ChildRoleBase.NONE;
    }
  }

  public int getTextOffset() {
    final XmlElement name = getNameElement();
    return name != null ? name.getTextOffset() : super.getTextOffset();
  }

  public XmlElement getNameElement() {
    return (XmlElement) findChildByRoleAsPsiElement(XmlChildRole.XML_NAME);
  }

  public XmlElementContentSpec getContentSpecElement() {
    return (XmlElementContentSpec) findChildByRoleAsPsiElement(XmlChildRole.XML_ELEMENT_CONTENT_SPEC);
  }

  public PsiMetaData getMetaData() {
    return MetaDataService.getInstance().getMeta(this);
  }

  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    XmlElementChangeUtil.doNameReplacement(this, getNameElement(), name);

    return null;
  }

  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this, XmlElementDecl.class);
  }

  public PsiElement getOriginalElement() {
    if (isPhysical()) return super.getOriginalElement();

    final PsiNamedElement element = XmlUtil.findRealNamedElement(this);

    if (element != null) {
      return element;
    }

    return this;
  }

  public boolean canNavigate() {
    if (!isPhysical()) {
      return getOriginalElement() != this;
    }

    return super.canNavigate();
  }

  public void navigate(boolean requestFocus) {
    if (!isPhysical()) {
      PsiElement element = getOriginalElement();

      if (element != this) {
        ((Navigatable) element).navigate(requestFocus);
        return;
      }
    }

    super.navigate(requestFocus);
  }

  public String getName() {
    XmlElement xmlElement = getNameElement();
    if (xmlElement != null) return xmlElement.getText();

    return getNameFromEntityRef(this, XML_ELEMENT_DECL_START);
  }

  @Override
  public boolean isEquivalentTo(final PsiElement another) {
    if (!(another instanceof XmlElementDecl)) return false;
    PsiElement element1 = this;
    PsiElement element2 = another;
    if (!element1.isPhysical()) element1 = element1.getOriginalElement();
    if (!element2.isPhysical()) element2 = element2.getOriginalElement();

    return element1 == element2;
  }

  public PsiElement getNameIdentifier() {
    return null;
  }

  @Nonnull
  public PsiElement getNavigationElement() {
    return this;
  }
}
