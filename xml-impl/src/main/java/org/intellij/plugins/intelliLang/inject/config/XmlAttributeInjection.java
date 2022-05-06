/*
 * Copyright 2006 Sascha Weinreuter
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
package org.intellij.plugins.intelliLang.inject.config;

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import consulo.ide.impl.idea.openapi.util.JDOMExternalizer;
import consulo.ide.impl.intelliLang.inject.InjectorUtils;
import consulo.ide.impl.intelliLang.inject.config.BaseInjection;
import consulo.ide.impl.intelliLang.inject.config.InjectionPlace;
import consulo.ide.impl.intelliLang.util.StringMatcher;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

public class XmlAttributeInjection extends AbstractTagInjection {

  @Nonnull
  @NonNls
  private StringMatcher myAttributeNameMatcher = StringMatcher.NONE;
  @Nonnull
  @NonNls
  private String myAttributeNamespace = "";

  @Nonnull
  public String getAttributeName() {
    return myAttributeNameMatcher.getPattern();
  }

  public void setAttributeName(@Nonnull String attributeName) {
    myAttributeNameMatcher = StringMatcher.create(attributeName);
  }

  @Nonnull
  public String getAttributeNamespace() {
    return myAttributeNamespace;
  }

  public void setAttributeNamespace(@Nonnull String attributeNamespace) {
    myAttributeNamespace = attributeNamespace;
  }

  public boolean isApplicable(@Nonnull XmlAttributeValue value) {
    final PsiElement element = value.getParent();
    return element instanceof XmlAttribute && matches((XmlAttribute)element);
  }

  @Nonnull
  public String getDisplayName() {
    final String tag = getTagName();
    final String attributeName = getAttributeName();
    if (!attributeName.equals(StringMatcher.NONE.getPattern())) {
      if (tag.length() > 0) {
        return tag + "/@" + (attributeName.length() > 0 ? attributeName : "*");
      }
      else {
        return "*/@" + (attributeName.length() > 0 ? attributeName : "*");
      }
    }
    return attributeName;
  }

  @Override
  public void generatePlaces() {
    setInjectionPlaces(new InjectionPlace(getCompiler().createElementPattern(getPatternString(this), getDisplayName()), true));
  }

  private boolean matches(@Nonnull XmlAttribute attr) {
    // mind IDEA-5206
    final boolean b = myAttributeNameMatcher.matches(attr.getLocalName()) &&
                      (attr.getName().indexOf(':') == -1 || myAttributeNamespace.equals(attr.getNamespace())) &&
                      matches(attr.getParent());

    return b && matchXPath(attr);
  }

  @Override
  public XmlAttributeInjection copy() {
    return new XmlAttributeInjection().copyFrom(this);
  }

  @Override
  public XmlAttributeInjection copyFrom(@Nonnull BaseInjection o) {
    super.copyFrom(o);
    if (o instanceof XmlAttributeInjection) {
      final XmlAttributeInjection other = (XmlAttributeInjection)o;
      setAttributeName(other.getAttributeName());
      setAttributeNamespace(other.getAttributeNamespace());
    }
    return this;
  }

  protected void readExternalImpl(Element e) {
    super.readExternalImpl(e);
    if (e.getAttribute("injector-id") == null) {
      setAttributeName(JDOMExternalizer.readString(e, "ATT_NAME"));
      setAttributeNamespace(JDOMExternalizer.readString(e, "ATT_NAMESPACE"));
    }
  }

  protected void writeExternalImpl(Element e) {
    super.writeExternalImpl(e);
  }

  @SuppressWarnings({"RedundantIfStatement"})
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final XmlAttributeInjection that = (XmlAttributeInjection)o;

    if (!myAttributeNameMatcher.getPattern().equals(that.myAttributeNameMatcher.getPattern())) return false;
    if (!myAttributeNamespace.equals(that.myAttributeNamespace)) return false;

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myAttributeNameMatcher.getPattern().hashCode();
    result = 31 * result + myAttributeNamespace.hashCode();
    return result;
  }

  public static String getPatternString(final XmlAttributeInjection injection) {
    final String name = injection.getAttributeName();
    final String namespace = injection.getAttributeNamespace();
    final StringBuilder result = new StringBuilder("xmlAttribute()");
    if (StringUtil.isNotEmpty(name)) InjectorUtils.appendStringPattern(result, ".withLocalName(", name, ")");
    if (StringUtil.isNotEmpty(namespace)) InjectorUtils.appendStringPattern(result, ".withNamespace(", namespace, ")");
    if (StringUtil.isNotEmpty(injection.getTagName()) || StringUtil.isNotEmpty(injection.getTagNamespace())) {
      result.append(".withParent(").append(XmlTagInjection.getPatternString(injection)).append(")");
    }
    return result.toString();
  }

}
