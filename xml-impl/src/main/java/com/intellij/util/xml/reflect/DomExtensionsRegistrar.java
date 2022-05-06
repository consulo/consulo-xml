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

import com.intellij.util.xml.XmlName;
import javax.annotation.Nonnull;

import java.lang.reflect.Type;

/**
 * @author peter
 */
public interface DomExtensionsRegistrar {

  @Nonnull
  DomExtension registerFixedNumberChildExtension(@Nonnull XmlName name, @Nonnull Type type);

  @Nonnull
  DomExtension registerCollectionChildrenExtension(@Nonnull XmlName name, @Nonnull Type type);

  @Nonnull
  DomExtension registerGenericAttributeValueChildExtension(@Nonnull XmlName name, final Type parameterType);

  /**
   * @param name attribute qualified name
   * @param type should extend GenericAttributeValue
   * @return dom extension object
   */
  @Nonnull
  DomExtension registerAttributeChildExtension(@Nonnull XmlName name, @Nonnull final Type type);

  /**
   * @param type
   * @return
   * @see com.intellij.util.xml.CustomChildren
   */
  @Nonnull
  DomExtension registerCustomChildrenExtension(@Nonnull final Type type);

  @Nonnull
  DomExtension registerCustomChildrenExtension(@Nonnull final Type type,
                                                        @Nonnull CustomDomChildrenDescription.TagNameDescriptor descriptor);

  @Nonnull
  DomExtension registerCustomChildrenExtension(@Nonnull final Type type,
                                                        @Nonnull CustomDomChildrenDescription.AttributeDescriptor attributeDescriptor);

}
