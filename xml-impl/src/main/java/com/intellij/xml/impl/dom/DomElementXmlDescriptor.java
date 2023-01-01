/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xml.impl.dom;

import consulo.language.pom.PomService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightingAwareElementDescriptor;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.DomChildrenDescription;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author mike
 */
public class DomElementXmlDescriptor extends AbstractDomChildrenDescriptor implements XmlHighlightingAwareElementDescriptor {
  private final DomChildrenDescription myChildrenDescription;

  public DomElementXmlDescriptor(@Nonnull final DomElement domElement) {
    super(domElement.getManager());
    myChildrenDescription = new MyRootDomChildrenDescription(domElement);
  }

  public DomElementXmlDescriptor(@Nonnull final DomChildrenDescription childrenDescription, final DomManager manager) {
    super(manager);
    myChildrenDescription = childrenDescription;
  }

  @Override
  public String getDefaultName() {
    return myChildrenDescription.getXmlElementName();
  }

  @Override
  @Nullable
  public PsiElement getDeclaration() {
    return myChildrenDescription.getDeclaration(myManager.getProject());
  }

  @Override
  @NonNls
  public String getName(final PsiElement context) {
    final String name = getDefaultName();
    if (context instanceof XmlTag) {
      XmlTag tag = (XmlTag) context;
      final PsiFile file = tag.getContainingFile();
      DomElement element = myManager.getDomElement(tag);
      if (element == null && tag.getParentTag() != null) {
        element = myManager.getDomElement(tag.getParentTag());
      }
      if (element != null && file instanceof XmlFile && !(myChildrenDescription instanceof MyRootDomChildrenDescription)) {
        final String namespace = DomService.getInstance().getEvaluatedXmlName(element).evaluateChildName(myChildrenDescription.getXmlName()).getNamespace(tag, (XmlFile) file);
        if (!tag.getNamespaceByPrefix("").equals(namespace)) {
          final String s = tag.getPrefixByNamespace(namespace);
          if (StringUtil.isNotEmpty(s)) {
            return s + ":" + name;
          }
        }
      }
    }

    return name;
  }

  @Override
  public boolean shouldCheckRequiredAttributes() {
    return false;
  }

  private static class MyRootDomChildrenDescription implements DomChildrenDescription {
    private final DomElement myDomElement;

    public MyRootDomChildrenDescription(final DomElement domElement) {
      myDomElement = domElement;
    }

    @Override
    public String getName() {
      return getXmlElementName();
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public void navigate(boolean requestFocus) {
    }

    @Override
    public boolean canNavigate() {
      return false;
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    @Nonnull
    public XmlName getXmlName() {
      throw new UnsupportedOperationException("Method getXmlName not implemented in " + getClass());
    }

    @Override
    @Nonnull
    public String getXmlElementName() {
      return myDomElement.getXmlElementName();
    }

    @Override
    @Nonnull
    public String getCommonPresentableName(@Nonnull final DomNameStrategy strategy) {
      throw new UnsupportedOperationException("Method getCommonPresentableName not implemented in " + getClass());
    }

    @Override
    @Nonnull
    public String getCommonPresentableName(@Nonnull final DomElement parent) {
      throw new UnsupportedOperationException("Method getCommonPresentableName not implemented in " + getClass());
    }

    @Override
    @Nonnull
    public List<? extends DomElement> getValues(@Nonnull final DomElement parent) {
      throw new UnsupportedOperationException("Method getValues not implemented in " + getClass());
    }

    @Override
    @Nonnull
    public List<? extends DomElement> getStableValues(@Nonnull final DomElement parent) {
      throw new UnsupportedOperationException("Method getStableValues not implemented in " + getClass());
    }

    @Override
    @Nonnull
    public Type getType() {
      throw new UnsupportedOperationException("Method getType not implemented in " + getClass());
    }

    @Override
    @Nonnull
    public DomNameStrategy getDomNameStrategy(@Nonnull final DomElement parent) {
      throw new UnsupportedOperationException("Method getDomNameStrategy not implemented in " + getClass());
    }

    @Override
    public <T> T getUserData(final Key<T> key) {
      return null;
    }

    @Override
    public ElementPresentationTemplate getPresentationTemplate() {
      return null;
    }

    @Override
    @Nullable
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
      throw new UnsupportedOperationException("Method getAnnotation not implemented in " + getClass());
    }

    @Override
    @Nullable
    public PsiElement getDeclaration(final Project project) {
      return PomService.convertToPsi(project, this);
    }

    @Override
    public DomElement getDomDeclaration() {
      return myDomElement;
    }

    @Override
    public boolean isStubbed() {
      return false;
    }
  }

}
