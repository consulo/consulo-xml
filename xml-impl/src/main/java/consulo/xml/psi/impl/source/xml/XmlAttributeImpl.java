/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.pom.PomManager;
import consulo.language.pom.PomModel;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;

import static consulo.language.editor.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;

/**
 * @author Mike
 */
public class XmlAttributeImpl extends XmlElementImpl implements XmlAttribute, HintedReferenceHost {
  private static final Logger LOG = Logger.getInstance("#XmlAttributeImpl");

  private final int myHC = ourHC++;

  @Override
  public final int hashCode() {
    return myHC;
  }

  public XmlAttributeImpl() {
    super(XmlElementType.XML_ATTRIBUTE);
  }

  @Override
  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == XmlTokenType.XML_NAME) {
      return XmlChildRole.XML_NAME;
    } else if (i == XmlElementType.XML_ATTRIBUTE_VALUE) {
      return XmlChildRole.XML_ATTRIBUTE_VALUE;
    } else {
      return 0;
    }
  }

  @Override
  public XmlAttributeValue getValueElement() {
    return (XmlAttributeValue) XmlChildRole.ATTRIBUTE_VALUE_FINDER.findChild(this);
  }

  @Override
  public void setValue(String valueText) throws IncorrectOperationException {
    final ASTNode value = XmlChildRole.ATTRIBUTE_VALUE_FINDER.findChild(this);
    final PomModel model = PomManager.getModel(getProject());
    final XmlAttribute attribute = XmlElementFactory.getInstance(getProject()).createXmlAttribute("a", valueText);
    final ASTNode newValue = XmlChildRole.ATTRIBUTE_VALUE_FINDER.findChild((ASTNode) attribute);

    if (value != null) {
      if (newValue != null) {
        replaceChild(value, newValue.copyElement());
      } else {
        removeChild(value);
      }
    } else {
      if (newValue != null) {
        addChild(newValue.copyElement());
      }
    }
  }

  @Override
  public XmlElement getNameElement() {
    return (XmlElement) XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(this);
  }

  @Override
  @Nonnull
  public String getNamespace() {
    final String name = getName();
    final String prefixByQualifiedName = XmlUtil.findPrefixByQualifiedName(name);
    // The namespace name for an unprefixed attribute name always has no value. Namespace recommendation section 6.2, third paragraph
    if (prefixByQualifiedName.isEmpty()) {
      return XmlUtil.EMPTY_URI;
    }
    return getParent().getNamespaceByPrefix(prefixByQualifiedName);
  }

  @Override
  @NonNls
  @Nonnull
  public String getNamespacePrefix() {
    return XmlUtil.findPrefixByQualifiedName(getName());
  }

  @Override
  public XmlTag getParent() {
    final PsiElement parentTag = super.getParent();
    return parentTag instanceof XmlTag ? (XmlTag) parentTag : null; // Invalid elements might belong to DummyHolder instead.
  }

  @Override
  @Nonnull
  public String getLocalName() {
    return XmlUtil.findLocalNameByQualifiedName(getName());
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor) visitor).visitXmlAttribute(this);
    } else {
      visitor.visitElement(this);
    }
  }

  @Override
  public String getValue() {
    final XmlAttributeValue valueElement = getValueElement();
    return valueElement != null ? valueElement.getValue() : null;
  }

  private volatile String myDisplayText = null;
  private volatile int[] myGapDisplayStarts = null;
  private volatile int[] myGapPhysicalStarts = null;
  private volatile TextRange myValueTextRange; // text inside quotes, if there are any

  protected void appendChildToDisplayValue(StringBuilder buffer, ASTNode child) {
    buffer.append(child.getChars());
  }

  @Override
  public String getDisplayValue() {
    String displayText = myDisplayText;
    if (displayText != null) {
      return displayText;
    }
    XmlAttributeValue value = getValueElement();
    if (value == null) {
      return null;
    }
    PsiElement firstChild = value.getFirstChild();
    if (firstChild == null) {
      return null;
    }
    ASTNode child = firstChild.getNode();
    TextRange valueTextRange = new TextRange(0, value.getTextLength());
    if (child != null && child.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
      valueTextRange = new TextRange(child.getTextLength(), valueTextRange.getEndOffset());
      child = child.getTreeNext();
    }
    final IntList gapsStarts = IntLists.newArrayList();
    final IntList gapsShifts = IntLists.newArrayList();
    StringBuilder buffer = new StringBuilder(getTextLength());
    while (child != null) {
      final int start = buffer.length();
      IElementType elementType = child.getElementType();
      if (elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
        valueTextRange = new TextRange(valueTextRange.getStartOffset(), child.getTextRange().getStartOffset() - value.getTextRange().getStartOffset());
        break;
      }
      if (elementType == XmlTokenType.XML_CHAR_ENTITY_REF) {
        buffer.append(XmlUtil.getCharFromEntityRef(child.getText()));
      } else if (elementType == XmlElementType.XML_ENTITY_REF) {
        buffer.append(XmlUtil.getEntityValue((XmlEntityRef) child));
      } else {
        appendChildToDisplayValue(buffer, child);
      }

      int end = buffer.length();
      int originalLength = child.getTextLength();
      if (end - start != originalLength) {
        gapsStarts.add(start);
        gapsShifts.add(originalLength - (end - start));
      }
      child = child.getTreeNext();
    }
    int[] gapDisplayStarts = ArrayUtil.newIntArray(gapsShifts.size());
    int[] gapPhysicalStarts = ArrayUtil.newIntArray(gapsShifts.size());
    int currentGapsSum = 0;
    for (int i = 0; i < gapDisplayStarts.length; i++) {
      currentGapsSum += gapsShifts.get(i);
      gapDisplayStarts[i] = gapsStarts.get(i);
      gapPhysicalStarts[i] = gapDisplayStarts[i] + currentGapsSum;
    }
    myGapDisplayStarts = gapDisplayStarts;
    myGapPhysicalStarts = gapPhysicalStarts;
    myValueTextRange = valueTextRange;
    return myDisplayText = buffer.toString();
  }

  @Override
  public int physicalToDisplay(int physicalIndex) {
    getDisplayValue();
    if (physicalIndex < 0 || physicalIndex > myValueTextRange.getLength()) {
      return -1;
    }
    if (myGapPhysicalStarts.length == 0) {
      return physicalIndex;
    }

    final int bsResult = Arrays.binarySearch(myGapPhysicalStarts, physicalIndex);

    final int gapIndex;
    if (bsResult > 0) {
      gapIndex = bsResult;
    } else if (bsResult < -1) {
      gapIndex = -bsResult - 2;
    } else {
      gapIndex = -1;
    }

    if (gapIndex < 0) {
      return physicalIndex;
    }
    final int shift = myGapPhysicalStarts[gapIndex] - myGapDisplayStarts[gapIndex];
    return Math.max(myGapDisplayStarts[gapIndex], physicalIndex - shift);
  }

  @Override
  public int displayToPhysical(int displayIndex) {
    String displayValue = getDisplayValue();
    if (displayValue == null || displayIndex < 0 || displayIndex > displayValue.length()) {
      return -1;
    }
    if (myGapDisplayStarts.length == 0) {
      return displayIndex;
    }

    final int bsResult = Arrays.binarySearch(myGapDisplayStarts, displayIndex);
    final int gapIndex;

    if (bsResult > 0) {
      gapIndex = bsResult - 1;
    } else if (bsResult < -1) {
      gapIndex = -bsResult - 2;
    } else {
      gapIndex = -1;
    }

    if (gapIndex < 0) {
      return displayIndex;
    }
    final int shift = myGapPhysicalStarts[gapIndex] - myGapDisplayStarts[gapIndex];
    return displayIndex + shift;
  }

  @Nonnull
  @Override
  public TextRange getValueTextRange() {
    getDisplayValue();
    return myValueTextRange;
  }

  @Override
  public void clearCaches() {
    super.clearCaches();
    myDisplayText = null;
    myGapDisplayStarts = null;
    myGapPhysicalStarts = null;
    myValueTextRange = null;
  }

  @Override
  @Nonnull
  public String getName() {
    XmlElement element = getNameElement();
    return element != null ? element.getText() : "";
  }

  @Override
  public boolean isNamespaceDeclaration() {
    @NonNls final String name = getName();
    return name.startsWith("xmlns:") || name.equals("xmlns");
  }

  @Override
  public PsiElement setName(@Nonnull final String nameText) throws consulo.language.util.IncorrectOperationException {
    final ASTNode name = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(this);
    final XmlAttribute attribute = XmlElementFactory.getInstance(getProject()).createXmlAttribute(nameText, "");
    final ASTNode newName = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild((ASTNode) attribute);
    CodeEditUtil.replaceChild(XmlAttributeImpl.this, name, newName);
    return this;
  }

  @Override
  public PsiReference getReference() {
    final PsiReference[] refs = getReferences(PsiReferenceService.Hints.NO_HINTS);
    if (refs.length > 0) {
      return refs[0];
    }
    return null;
  }

  @Override
  public boolean shouldAskParentForReferences(@Nonnull PsiReferenceService.Hints hints) {
    return false;
  }

  /**
   * Use {@link #getReferences(PsiReferenceService.Hints)} instead of calling or overriding this method.
   */
  @Deprecated
  @Nonnull
  @Override
  public final PsiReference[] getReferences() {
    return getReferences(PsiReferenceService.Hints.NO_HINTS);
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public PsiReference[] getReferences(@Nonnull PsiReferenceService.Hints hints) {
    if (hints.offsetInElement != null) {
      XmlElement nameElement = getNameElement();
      if (nameElement == null || hints.offsetInElement > nameElement.getStartOffsetInParent() + nameElement.getTextLength()) {
        return PsiReference.EMPTY_ARRAY;
      }
    }

    final PsiReference[] referencesFromProviders = ReferenceProvidersRegistry.getReferencesFromProviders(this);
    PsiReference[] refs;
    if (isNamespaceDeclaration()) {
      refs = new PsiReference[referencesFromProviders.length + 1];
      final String localName = getLocalName();
      final String prefix = XmlUtil.findPrefixByQualifiedName(getName());
      final TextRange range = prefix.isEmpty() ? TextRange.from(getName().length(), 0) : TextRange.from(prefix.length() + 1, localName.length());
      refs[0] = new SchemaPrefixReference(this, range, localName, null);
    } else {
      final String prefix = getNamespacePrefix();
      if (!prefix.isEmpty() && !getLocalName().isEmpty()) {
        refs = new PsiReference[referencesFromProviders.length + 2];
        refs[0] = new SchemaPrefixReference(this, TextRange.from(0, prefix.length()), prefix, null);
        refs[1] = new XmlAttributeReference(this);
      } else {
        refs = new PsiReference[referencesFromProviders.length + 1];
        refs[0] = new XmlAttributeReference(this);
      }
    }
    System.arraycopy(referencesFromProviders, 0, refs, refs.length - referencesFromProviders.length, referencesFromProviders.length);
    return refs;
  }

  @Override
  @Nullable
  public XmlAttributeDescriptor getDescriptor() {
    final PsiElement parentElement = getParent();
    if (parentElement instanceof XmlDecl) {
      return null;
    }
    final XmlTag tag = (XmlTag) parentElement;
    final XmlElementDescriptor descr = tag.getDescriptor();
    if (descr == null) {
      return null;
    }
    final XmlAttributeDescriptor attributeDescr = descr.getAttributeDescriptor(this);
    return attributeDescr == null ? descr.getAttributeDescriptor(getName(), tag) : attributeDescr;
  }

  public String getRealLocalName() {
    final String name = getLocalName();
    return name.endsWith(DUMMY_IDENTIFIER_TRIMMED) ? name.substring(0, name.length() - DUMMY_IDENTIFIER_TRIMMED.length()) : name;
  }
}
