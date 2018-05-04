/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.util.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.reflect.DomGenericInfo;
import com.intellij.util.xml.reflect.AbstractDomChildrenDescription;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author peter
 */
public class MockDomElement extends UserDataHolderBase implements DomElement{
  @Override
  @Nullable
  public XmlTag getXmlTag() {
    throw new UnsupportedOperationException("Method getXmlTag is not yet implemented in " + getClass().getName());
  }

  @Nonnull
  public <T extends DomElement> DomFileElement<T> getRoot() {
    throw new UnsupportedOperationException("Method getRoot is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public DomElement getParent() {
    throw new UnsupportedOperationException("Method getParent is not yet implemented in " + getClass().getName());
  }

  @Override
  public XmlTag ensureTagExists() {
    throw new UnsupportedOperationException("Method ensureTagExists is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public XmlElement getXmlElement() {
    throw new UnsupportedOperationException("Method getXmlElement is not yet implemented in " + getClass().getName());
  }

  @Override
  public XmlElement ensureXmlElementExists() {
    throw new UnsupportedOperationException("Method ensureXmlElementExists is not yet implemented in " + getClass().getName());
  }

  @Override
  public void undefine() {
    throw new UnsupportedOperationException("Method undefine is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isValid() {
    throw new UnsupportedOperationException("Method isValid is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean exists() {
    throw new UnsupportedOperationException("Method exists is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public DomGenericInfo getGenericInfo() {
    throw new UnsupportedOperationException("Method getGenericInfo is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  @NonNls
  public String getXmlElementName() {
    throw new UnsupportedOperationException("Method getXmlElementName is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  @NonNls
  public String getXmlElementNamespace() {
    throw new UnsupportedOperationException("Method getXmlElementNamespace is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  @NonNls
  public String getXmlElementNamespaceKey() {
    throw new UnsupportedOperationException("Method getXmlElementNamespaceKey is not yet implemented in " + getClass().getName());
  }

  @Override
  public void accept(final DomElementVisitor visitor) {
    throw new UnsupportedOperationException("Method accept is not yet implemented in " + getClass().getName());
  }

  @Override
  public void acceptChildren(DomElementVisitor visitor) {
    throw new UnsupportedOperationException("Method acceptChildren is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public DomManager getManager() {
    throw new UnsupportedOperationException("Method getManager is not yet implemented in " + getClass().getName());
  }

  @Override
  public Type getDomElementType() {
    throw new UnsupportedOperationException("Method getDomElementType is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public AbstractDomChildrenDescription getChildDescription() {
    throw new UnsupportedOperationException("Method getChildDescription is not yet implemented in " + getClass().getName());
  }

  @Override
  public DomNameStrategy getNameStrategy() {
    throw new UnsupportedOperationException("Method getNameStrategy is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public ElementPresentation getPresentation() {
    throw new UnsupportedOperationException("Method getPresentation is not yet implemented in " + getClass().getName());
  }

  @Override
  public GlobalSearchScope getResolveScope() {
    throw new UnsupportedOperationException("Method getResolveScope is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public <T extends DomElement> T getParentOfType(Class<T> requiredClass, boolean strict) {
    throw new UnsupportedOperationException("Method getParentOfType is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public Module getModule() {
    throw new UnsupportedOperationException("Method getModule is not yet implemented in " + getClass().getName());
  }

  @Override
  public void copyFrom(DomElement other) {
    throw new UnsupportedOperationException("Method copyFrom is not yet implemented in " + getClass().getName());
  }

  @Override
  public <T extends DomElement> T createMockCopy(final boolean physical) {
    throw new UnsupportedOperationException("Method createMockCopy is not yet implemented in " + getClass().getName());
  }

  @Override
  public <T extends DomElement> T createStableCopy() {
    return (T)this;
  }

  @Override
  @Nullable
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    throw new UnsupportedOperationException("Method getAnnotation is not yet implemented in " + getClass().getName());
  }
}
