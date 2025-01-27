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
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.meta.MetaDataService;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.EditSourceUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * @author Mike
 */
public class XmlAttributeDeclImpl extends XmlElementImpl implements XmlAttributeDecl, XmlElementType {
  private static final Logger LOG = Logger.getInstance("#XmlAttributeDeclImpl");
  @NonNls
  private static final String ID_ATT = "ID";
  @NonNls
  private static final String IDREF_ATT = "IDREF";

  public XmlAttributeDeclImpl() {
    super(XML_ATTRIBUTE_DECL);
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == XML_NAME) {
      return XmlChildRole.XML_NAME;
    } else if (i == XML_ATT_REQUIRED) {
      return XmlChildRole.XML_ATT_REQUIRED;
    } else if (i == XML_ATT_FIXED) {
      return XmlChildRole.XML_ATT_FIXED;
    } else if (i == XML_ATT_IMPLIED) {
      return XmlChildRole.XML_ATT_IMPLIED;
    } else if (i == XML_ATTRIBUTE_VALUE) {
      return XmlChildRole.XML_DEFAULT_VALUE;
    } else if (i == XML_ENUMERATED_TYPE) {
      return XmlChildRole.XML_ENUMERATED_TYPE;
    } else {
      return 0;
    }
  }

  public XmlElement getNameElement() {
    return findElementByTokenType(XML_NAME);
  }

  public boolean isAttributeRequired() {
    return findElementByTokenType(XML_ATT_REQUIRED) != null;
  }

  public boolean isAttributeFixed() {
    return findElementByTokenType(XML_ATT_FIXED) != null;
  }

  public boolean isAttributeImplied() {
    return findElementByTokenType(XML_ATT_IMPLIED) != null;
  }

  public XmlAttributeValue getDefaultValue() {
    return (XmlAttributeValue) findElementByTokenType(XML_ATTRIBUTE_VALUE);
  }

  public String getDefaultValueText() {
    XmlAttributeValue value = getDefaultValue();
    if (value == null) return null;
    String text = value.getText();
    if (text.indexOf('%') == -1 && text.indexOf('&') == -1) return text;

    final StringBuilder builder = new StringBuilder();
    value.processElements(new PsiElementProcessor() {
      public boolean execute(@Nonnull PsiElement element) {
        builder.append(element.getText());
        return true;
      }
    }, null);
    return builder.toString();
  }

  public boolean isEnumerated() {
    return findElementByTokenType(XML_ENUMERATED_TYPE) != null;
  }

  public XmlElement[] getEnumeratedValues() {
    XmlEnumeratedType enumeratedType = (XmlEnumeratedType) findElementByTokenType(XML_ENUMERATED_TYPE);
    if (enumeratedType != null) {
      return enumeratedType.getEnumeratedValues();
    } else {
      return XmlElement.EMPTY_ARRAY;
    }
  }

  public boolean isIdAttribute() {
    final PsiElement elementType = findElementType();

    return elementType != null && elementType.getText().equals(ID_ATT);
  }

  private PsiElement findElementType() {
    final PsiElement elementName = findElementByTokenType(XML_NAME);
    final PsiElement nextSibling = (elementName != null) ? elementName.getNextSibling() : null;
    final PsiElement elementType = (nextSibling instanceof PsiWhiteSpace) ? nextSibling.getNextSibling() : nextSibling;

    return elementType;
  }

  public boolean isIdRefAttribute() {
    final PsiElement elementType = findElementType();

    return elementType != null && elementType.getText().equals(IDREF_ATT);
  }

  public PsiMetaData getMetaData() {
    return MetaDataService.getInstance().getMeta(this);
  }

  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    XmlElementChangeUtil.doNameReplacement(this, getNameElement(), name);
    return null;
  }

  public String getName() {
    XmlElement name = getNameElement();
    return (name != null) ? name.getText() : null;
  }

  public boolean canNavigate() {
    if (isPhysical()) return super.canNavigate();
    final PsiNamedElement psiNamedElement = XmlUtil.findRealNamedElement(this);
    return psiNamedElement != null && psiNamedElement != this && ((Navigatable) psiNamedElement).canNavigate();
  }

  public void navigate(final boolean requestFocus) {
    if (isPhysical()) {
      super.navigate(requestFocus);
      return;
    }
    final PsiNamedElement psiNamedElement = XmlUtil.findRealNamedElement(this);
    Navigatable navigatable = EditSourceUtil.getDescriptor(psiNamedElement);

    if (psiNamedElement instanceof XmlEntityDecl) {
      final OpenFileDescriptor fileDescriptor = (OpenFileDescriptor) navigatable;
      navigatable = OpenFileDescriptorFactory.getInstance(
          fileDescriptor.getProject()).builder(fileDescriptor.getFile()).offset(psiNamedElement.getTextRange().getStartOffset() + psiNamedElement.getText().indexOf(getName())).build();
    }
    navigatable.navigate(requestFocus);
  }

  @Nonnull
  public PsiElement getNavigationElement() {
    return this;
  }
}
