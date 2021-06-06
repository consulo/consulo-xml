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
package com.intellij.util.xml.reflect;

import com.intellij.util.ParameterizedTypeImpl;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.XmlName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
public class DomExtensionsRegistrarImpl implements DomExtensionsRegistrar {
  private final List<DomExtensionImpl> myAttributes = new SmartList<DomExtensionImpl>();
  private final List<DomExtensionImpl> myFixeds = new SmartList<DomExtensionImpl>();
  private final List<DomExtensionImpl> myCollections = new SmartList<DomExtensionImpl>();
  private final Set<Object> myDependencies = new HashSet<Object>();
  private final List<DomExtensionImpl> myCustoms = new SmartList<DomExtensionImpl>();

  public List<DomExtensionImpl> getAttributes() {
    return myAttributes;
  }
  public List<DomExtensionImpl> getFixeds() {
    return myFixeds;
  }

  public List<DomExtensionImpl> getCollections() {
    return myCollections;
  }

  public List<DomExtensionImpl> getCustoms() {
    return myCustoms;
  }

  @Nonnull
  public final DomExtension registerFixedNumberChildrenExtension(@Nonnull final XmlName name, @Nonnull final Type type, final int count) {
    assert count > 0;
    return addExtension(myFixeds, name, type).setCount(count);
  }

  @Nonnull
  public DomExtension registerFixedNumberChildExtension(@Nonnull final XmlName name, @Nonnull final Type type) {
    return registerFixedNumberChildrenExtension(name, type, 1);
  }

  @Nonnull
  public DomExtension registerCollectionChildrenExtension(@Nonnull final XmlName name, @Nonnull final Type type) {
    return addExtension(myCollections, name, type);
  }

  @Nonnull
  public DomExtension registerGenericAttributeValueChildExtension(@Nonnull final XmlName name, final Type parameterType) {
    return addExtension(myAttributes, name, new ParameterizedTypeImpl(GenericAttributeValue.class, parameterType));
  }

  @Nonnull
  public DomExtension registerAttributeChildExtension(@Nonnull final XmlName name, @Nonnull final Type type) {
    assert GenericAttributeValue.class.isAssignableFrom(ReflectionUtil.getRawType(type));
    return addExtension(myAttributes, name, type);
  }

  @Nonnull
  public DomExtension registerCustomChildrenExtension(@Nonnull final Type type) {
    return registerCustomChildrenExtension(type, CustomDomChildrenDescription.TagNameDescriptor.EMPTY);
  }

  @Nonnull
  @Override
  public DomExtension registerCustomChildrenExtension(@Nonnull Type type,
                                                      @Nonnull CustomDomChildrenDescription.TagNameDescriptor descriptor) {
    DomExtensionImpl extension = addExtension(myCustoms, null, type);
    extension.setTagNameDescriptor(descriptor);
    return extension;
  }

  @Nonnull
  @Override
  public DomExtension registerCustomChildrenExtension(@Nonnull Type type,
                                                      @Nonnull CustomDomChildrenDescription.AttributeDescriptor attributeDescriptor) {

    DomExtensionImpl extension = addExtension(myCustoms, null, type);
    extension.setAttributesDescriptor(attributeDescriptor);
    return extension;
  }

  private static DomExtensionImpl addExtension(final List<DomExtensionImpl> list, @Nullable final XmlName name, final Type type) {
    final DomExtensionImpl extension = new DomExtensionImpl(type, name);
    list.add(extension);
    return extension;
  }

  public final void addDependencies(Object[] deps) {
    ContainerUtil.addAll(myDependencies, deps);
  }

  public Object[] getDependencies() {
    return myDependencies.toArray();
  }
}
