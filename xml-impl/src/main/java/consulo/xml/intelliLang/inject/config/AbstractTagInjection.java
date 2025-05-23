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
package consulo.xml.intelliLang.inject.config;

import consulo.language.inject.advanced.BaseInjection;
import consulo.language.inject.advanced.StringMatcher;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.JDOMExternalizer;
import consulo.xml.XPathSupportProvider;
import consulo.xml.intelliLang.inject.xml.XmlLanguageInjectionSupport;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for XML-related injections (XML tags and attributes).
 * Contains the tag's local name and namespace-uri, an optional value-pattern
 * and an optional XPath expression (only valid if XPathView is installed) and
 * the appropriate logic to determine if a tag matches those properties.
 *
 * @see XPathSupportProvider
 */
public class AbstractTagInjection extends BaseInjection {

  private static final Logger LOG = Logger.getInstance("AbstractTagInjection");

  @Nonnull
  @NonNls
  private StringMatcher myTagName = StringMatcher.ANY;

  @Nonnull
  @NonNls
  private Set<String> myTagNamespace = Collections.emptySet();
  @Nonnull
  @NonNls
  private String myXPathCondition = "";

  private XPath myCompiledXPathCondition;
  private boolean myApplyToSubTagTexts;

  public AbstractTagInjection() {
    super(XmlLanguageInjectionSupport.XML_SUPPORT_ID);
  }

  @Nonnull
  public String getTagName() {
    return myTagName.getPattern();
  }

  public void setTagName(@Nonnull @NonNls String tagName) {
    myTagName = StringMatcher.create(tagName);
  }

  @Override
  public boolean acceptsPsiElement(final PsiElement element) {
    return super.acceptsPsiElement(element) &&
           (!(element instanceof XmlElement) || matchXPath((XmlElement)element));
  }

  @Nonnull
  public String getTagNamespace() {
    return StringUtil.join(myTagNamespace, "|");
  }

  public void setTagNamespace(@Nonnull @NonNls String tagNamespace) {
    myTagNamespace = new TreeSet<String>(StringUtil.split(tagNamespace,"|"));
  }

  @Nonnull
  public String getXPathCondition() {
    return myXPathCondition;
  }

  @Nullable
  public XPath getCompiledXPathCondition() {
    return myCompiledXPathCondition;
  }

  public void setXPathCondition(@Nullable String condition) {
    myXPathCondition = condition != null ? condition : "";
    if (StringUtil.isNotEmpty(myXPathCondition)) {
      try {
        final XPathSupportProvider xPathSupport = XPathSupportProvider.findProvider();
        if (xPathSupport != null) {
          myCompiledXPathCondition = xPathSupport.createXPath(myXPathCondition);
        }
        else {
          myCompiledXPathCondition = null;
        }
      }
      catch (JaxenException e) {
        myCompiledXPathCondition = null;
        LOG.warn("Invalid XPath expression", e);
      }
    }
    else {
      myCompiledXPathCondition = null;
    }
  }

  @SuppressWarnings({"RedundantIfStatement"})
  protected boolean matches(@Nullable XmlTag tag) {
    if (tag == null) {
      return false;
    }
    if (!myTagName.matches(tag.getLocalName())) {
      return false;
    }
    if (!myTagNamespace.contains(tag.getNamespace())) {
      return false;
    }
    return true;
  }

  @Override
  public AbstractTagInjection copy() {
    return new AbstractTagInjection().copyFrom(this);
  }

  public AbstractTagInjection copyFrom(@Nonnull BaseInjection o) {
    super.copyFrom(o);
    if (o instanceof AbstractTagInjection) {
      final AbstractTagInjection other = (AbstractTagInjection)o;
      myTagName = other.myTagName;
      myTagNamespace = other.myTagNamespace;
      setXPathCondition(other.getXPathCondition());

      setApplyToSubTagTexts(other.isApplyToSubTagTexts());
    }
    return this;
  }

  protected void readExternalImpl(Element e) {
    if (e.getAttribute("injector-id") == null) {
      setTagName(JDOMExternalizer.readString(e, "TAGNAME"));
      setTagNamespace(JDOMExternalizer.readString(e, "TAGNAMESPACE"));
      setXPathCondition(JDOMExternalizer.readString(e, "XPATH_CONDITION"));

      myApplyToSubTagTexts = JDOMExternalizer.readBoolean(e, "APPLY_TO_SUBTAGS");
    }
    else {
      setXPathCondition(e.getChildText("xpath-condition"));
      myApplyToSubTagTexts = e.getChild("apply-to-subtags") != null;
    }
  }

  protected void writeExternalImpl(Element e) {
    if (StringUtil.isNotEmpty(myXPathCondition)) {
      e.addContent(new Element("xpath-condition").setText(myXPathCondition));
    }
    if (myApplyToSubTagTexts) {
      e.addContent(new Element("apply-to-subtags"));
    }
  }

  @SuppressWarnings({"RedundantIfStatement"})
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final AbstractTagInjection that = (AbstractTagInjection)o;

    if (!myTagName.equals(that.myTagName)) return false;
    if (!myTagNamespace.equals(that.myTagNamespace)) return false;
    if (!myXPathCondition.equals(that.myXPathCondition)) return false;

    if (myApplyToSubTagTexts != that.myApplyToSubTagTexts) return false;
    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myTagName.hashCode();
    result = 31 * result + myTagNamespace.hashCode();
    result = 31 * result + myXPathCondition.hashCode();

    result = 31 * result + (myApplyToSubTagTexts ? 1 : 0);
    return result;
  }

  protected boolean matchXPath(XmlElement context) {
    final XPath condition = getCompiledXPathCondition();
    if (condition != null) {
      try {
        return condition.booleanValueOf(context);
      }
      catch (JaxenException e) {
        LOG.warn(e);
        myCompiledXPathCondition = null;
        return false;
      }
    }
    return myXPathCondition.length() == 0;
  }

  public boolean isApplyToSubTagTexts() {
    return myApplyToSubTagTexts;
  }

  public void setApplyToSubTagTexts(final boolean applyToSubTagTexts) {
    myApplyToSubTagTexts = applyToSubTagTexts;
  }

  @Override
  public boolean acceptForReference(PsiElement element) {
    if (element instanceof XmlAttributeValue) {
      PsiElement parent = element.getParent();
      return parent instanceof XmlAttribute && acceptsPsiElement(parent);
    }
    else return element instanceof XmlTag && acceptsPsiElement(element);
  }
}
