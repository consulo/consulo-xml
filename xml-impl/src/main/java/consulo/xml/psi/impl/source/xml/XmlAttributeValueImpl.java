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

import javax.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.language.psi.*;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.navigation.ItemPresentation;
import consulo.navigation.ItemPresentationWithSeparator;
import consulo.logging.Logger;
import consulo.document.util.TextRange;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.XmlElementVisitor;
import consulo.language.impl.psi.CheckUtil;
import consulo.xml.psi.impl.source.tree.injected.XmlAttributeLiteralEscaper;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;
import consulo.util.collection.ArrayUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;

/**
 * @author Mike
 */
public class XmlAttributeValueImpl extends XmlElementImpl implements XmlAttributeValue, PsiLanguageInjectionHost, PsiMetaOwner, PsiMetaData {
  private static final Logger LOG = Logger.getInstance("#XmlAttributeValueImpl");
  private volatile PsiReference[] myCachedReferences;
  private volatile long myModCount;

  public XmlAttributeValueImpl() {
    super(XmlElementType.XML_ATTRIBUTE_VALUE);
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor)visitor).visitXmlAttributeValue(this);
    }
    else {
      visitor.visitElement(this);
    }
  }

  @Override
  public String getValue() {
    // it is more correct way to strip quotes since injected xml may have quotes encoded
    String text = getText();
    ASTNode startQuote = findChildByType(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER);
    if (startQuote != null) {
      text = StringUtil.trimStart(text, startQuote.getText());
    }
    ASTNode endQuote = findChildByType(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER);
    if (endQuote != null) {
      text = StringUtil.trimEnd(text, endQuote.getText());
    }
    return text;
  }

  @Override
  public TextRange getValueTextRange() {
    final TextRange range = getTextRange();
    final String value = getValue();
    if (value.length() == 0) {
      return range; 
    }
    final int start = range.getStartOffset() + getText().indexOf(value);
    final int end = start + value.length();
    return new TextRange(start, end);
  }

  @Override
  public void clearCaches() {
    super.clearCaches();
    myCachedReferences = null;
  }

  @Override
  @Nonnull
  public PsiReference[] getReferences() {
    PsiReference[] cachedReferences = myCachedReferences;
    final long curModCount = getManager().getModificationTracker().getModificationCount();
    if (cachedReferences != null && myModCount == curModCount) {
      return cachedReferences;
    }
    cachedReferences = ReferenceProvidersRegistry.getReferencesFromProviders(this);
    myCachedReferences = cachedReferences;
    myModCount = curModCount;
    return cachedReferences;
  }

  @Override
  public PsiReference getReference() {
    final PsiReference[] refs = getReferences();
    if (refs.length > 0) return refs[0];
    return null;
  }


  @Override
  public int getTextOffset() {
    return getTextRange().getStartOffset() + 1;
  }

  @Override
  public boolean isValidHost() {
    return getParent() instanceof XmlAttributeImpl;
  }

  @Override
  public PsiLanguageInjectionHost updateText(@Nonnull String text) {
    try {
      final String quoteChar = getTextLength() > 0 ? getText().substring(0, 1) : "";
      String contents = StringUtil.containsAnyChar(quoteChar, "'\"") ?
              StringUtil.trimEnd(StringUtil.trimStart(text, quoteChar), quoteChar) : text;
      XmlAttribute newAttribute = XmlElementFactory.getInstance(getProject()).createXmlAttribute("q", contents);
      XmlAttributeValue newValue = newAttribute.getValueElement();

      CheckUtil.checkWritable(this);
      replaceAllChildrenToChildrenOf(newValue.getNode());
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
    return this;
  }

  @Override
  @Nonnull
  public LiteralTextEscaper<XmlAttributeValueImpl> createLiteralTextEscaper() {
    return new XmlAttributeLiteralEscaper(this);
  }

  @Override
  public PsiMetaData getMetaData() {
    return this;
  }

  @Override
  public PsiElement getDeclaration() {
    return this;
  }

  @Override
  public String getName(final PsiElement context) {
    return getValue();
  }

  @Override
  public String getName() {
    return getValue();
  }

  @Override
  public void init(final PsiElement element) {
  }

  @Override
  public Object[] getDependences() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public ItemPresentation getPresentation() {
    return new ItemPresentationWithSeparator() {
      @Override
      public String getPresentableText() {
        return getText();
      }

      @Override
      public String getLocationString() {
        return SymbolPresentationUtil.getFilePathPresentation(getContainingFile());
      }

      @Override
      public Image getIcon() {
        return null;
      }
    };
  }
}
