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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.document.util.TextRange;
import consulo.language.impl.psi.RenameableFakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import com.intellij.xml.XmlExtension;
import consulo.application.AllIcons;
import consulo.language.psi.ElementManipulators;
import consulo.ui.image.Image;

/**
 * @author Konstantin Bulenkov
 */
public class SchemaPrefix extends RenameableFakePsiElement {
  private final TextRange myRange;
  private final String myName;

  public SchemaPrefix(final XmlAttribute parent, TextRange range, String name) {
    super(parent);
    myRange = range;
    myName = name;
  }

  public static SchemaPrefix createJspPrefix(XmlAttributeValue element, String prefix) {
    TextRange range = ElementManipulators.getValueTextRange(element).shiftRight(element.getStartOffsetInParent());
    return new SchemaPrefix((XmlAttribute)element.getParent(), range, prefix) {
      @Override
      protected String getNamespace() {
        return ((XmlAttribute)getParent()).getParent().getAttributeValue("uri");
      }
    };
  }

  public String getTypeName() {
    return "XML Namespace Prefix";
  }

  public Image getIcon() {
    return AllIcons.Nodes.Variable;
  }

  @Override
  public int getTextOffset() {
    return getParent().getTextRange().getStartOffset() + myRange.getStartOffset();
  }

  @Override
  public int getTextLength() {
    return myName.length();
  }

  @Override
  public String getName() {
    return myName;
  }

  public XmlAttribute getDeclaration() {
    return (XmlAttribute)getParent();
  }

  @Override
  public TextRange getTextRange() {
    return TextRange.from(getTextOffset(), getTextLength());
  }

  @Nonnull
  @Override
  public SearchScope getUseScope() {
    return XmlExtension.getExtension(getContainingFile()).getNsPrefixScope(getDeclaration());
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return another instanceof SchemaPrefix && ((SchemaPrefix)another).getDeclaration() == getDeclaration();
  }

  public String getQuickNavigateInfo() {
    String ns = getNamespace();
    StringBuilder builder = new StringBuilder().append(getTypeName()).append(" \"").append(getName()).append("\"");
    if (ns != null) {
      builder.append(" (").append(ns).append(")");
    }
    return builder.toString();
  }

  @Nullable
  protected String getNamespace() {
    XmlAttribute parent = (XmlAttribute)getParent();
    return parent.getValue();
  }
}
