/*
 * Copyright 2007 Sascha Weinreuter
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

package org.intellij.plugins.relaxNG.references;

import consulo.xml.patterns.XmlAttributeValuePattern;
import consulo.xml.patterns.XmlNamedElementPattern;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.util.ProcessingContext;
import com.intellij.xml.util.XmlUtil;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

class ResolvingVisitor extends XmlElementVisitor implements PsiElementProcessor {
  static final Key<Set<XmlFile>> VISITED_KEY = Key.create("visited");

  private final XmlAttributeValuePattern myPattern;
  private final ProcessingContext myProcessingContext;
  private XmlNamedElementPattern myIncludePattern;

  public ResolvingVisitor(XmlAttributeValuePattern pattern, ProcessingContext context) {
    myPattern = pattern;
    myProcessingContext = context;

    myProcessingContext.put(VISITED_KEY, new HashSet<>());
  }

  @Override
  public void visitXmlDocument(@Nullable XmlDocument document) {
    if (document != null) {
      final XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        visitXmlTag(rootTag);
      }
    }
  }

  public void setIncludePattern(XmlNamedElementPattern includePattern) {
    myIncludePattern = includePattern;
  }

  @Override
  public void visitXmlAttribute(XmlAttribute attribute) {
    if (myIncludePattern != null && myIncludePattern.accepts(attribute, myProcessingContext)) {
      final String value = attribute.getValue();
      if (value == null) return;

      final XmlFile xmlFile = XmlUtil.findXmlFile(attribute.getContainingFile(), value);
      if (xmlFile != null) {
        processInclude(xmlFile, attribute);
      }
    } else {
      processAttribute(attribute);
    }
  }

  private void processAttribute(XmlAttribute attribute) {
    myPattern.accepts(attribute.getValueElement(), myProcessingContext);
  }

  @Override
  @SuppressWarnings({ "ForLoopReplaceableByForEach" })
  public void visitXmlTag(XmlTag tag) {
    visitAttributes(tag);
  }

  protected void visitAttributes(XmlTag tag) {
    final XmlAttribute[] xmlAttributes = tag.getAttributes();
    for (XmlAttribute attribute : xmlAttributes) {
      attribute.accept(this);
    }
  }

  protected void visitSubTags(XmlTag tag) {
    final XmlTag[] tags = tag.getSubTags();
    for (XmlTag subTag : tags) {
      if (shouldContinue()) {
        subTag.accept(this);
      }
    }
  }

  private void processInclude(XmlFile xmlFile, XmlAttribute attribute) {
    final Set<XmlFile> set = myProcessingContext.get(VISITED_KEY);
    if (set.contains(xmlFile)) {
      return;
    }
    set.add(xmlFile);

    final XmlDocument document = xmlFile.getDocument();
    if (document == null) return;
    final XmlTag rootTag = document.getRootTag();
    if (rootTag == null) return;

    rootTag.processElements(this, attribute);
  }

  @Override
  public boolean execute(@Nonnull PsiElement element) {
    element.accept(this);
    return shouldContinue();
  }

  protected boolean shouldContinue() {
    return true;
  }
}