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

import consulo.ide.impl.psi.tree.ChildRoleBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;
import consulo.language.psi.resolve.FilterElementProcessor;
import consulo.logging.Logger;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.ast.ASTNode;
import consulo.xml.psi.xml.*;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike
 */
public class XmlAttlistDeclImpl extends XmlElementImpl implements XmlAttlistDecl {
  private static final Logger LOG = Logger.getInstance("#XmlAttlistDeclImpl");

  public XmlAttlistDeclImpl() {
    super(XmlElementType.XML_ATTLIST_DECL);
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    if (child.getElementType() == XmlElementType.XML_NAME) {
      return XmlChildRole.XML_NAME;
    }
    else {
      return ChildRoleBase.NONE;
    }
  }

  public XmlElement getNameElement() {
    return (XmlElement)findChildByRoleAsPsiElement(XmlChildRole.XML_NAME);
  }

  public XmlAttributeDecl[] getAttributeDecls() {
    final List<XmlAttributeDecl> result = new ArrayList<XmlAttributeDecl>();
    processElements(new FilterElementProcessor(new ClassFilter(XmlAttributeDecl.class), result) {
      public boolean execute(@Nonnull final PsiElement element) {
        if (element instanceof XmlAttributeDecl) {
          if (element.getNextSibling() == null && element.getChildren().length == 1) {
            return true;
          }
          return super.execute(element);
        }
        return true;
      }
    }, this);
    return result.toArray(new XmlAttributeDecl[result.size()]);
  }

  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this,XmlAttlistDecl.class);
  }

  public String getName() {
    XmlElement xmlElement = getNameElement();
    if (xmlElement != null) return xmlElement.getText();

    return getNameFromEntityRef(this, XmlElementType.XML_ATTLIST_DECL_START);
  }
}
