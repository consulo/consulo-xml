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
package consulo.xml.util.xml.impl;

import consulo.util.collection.ArrayUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.CustomDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomExtensionImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author peter
 */
public class CustomDomChildrenDescriptionImpl extends AbstractDomChildDescriptionImpl implements CustomDomChildrenDescription, AbstractCollectionChildDescription {
  @Nullable
  private final JavaMethod myGetter;
  public static final Function<DomInvocationHandler,List<XmlTag>> CUSTOM_TAGS_GETTER = new Function<DomInvocationHandler, List<XmlTag>>() {
    @Nonnull
    public List<XmlTag> apply(final DomInvocationHandler handler) {
      return DomImplUtil.getCustomSubTags(handler, handler.getXmlTag().getSubTags(), handler.getFile());
    }
  };

  private final TagNameDescriptor myTagNameDescriptor;
  private final AttributeDescriptor myAttributeDescriptor;


  public CustomDomChildrenDescriptionImpl(@Nonnull final JavaMethod getter) {
    this(getter, DomReflectionUtil.extractCollectionElementType(getter.getGenericReturnType()),
         TagNameDescriptor.EMPTY, TagNameDescriptor.EMPTY);
  }

  public CustomDomChildrenDescriptionImpl(DomExtensionImpl custom) {
    this(null, custom.getType(), custom.getTagNameDescriptor(), custom.getAttributesDescriptor());
  }

  private CustomDomChildrenDescriptionImpl(@Nullable final JavaMethod getter, @Nonnull Type type,
                                          @Nullable TagNameDescriptor descriptor,
                                          @Nullable AttributeDescriptor attributesDescriptor) {
    super(type);
    myGetter = getter;
    myTagNameDescriptor = descriptor;
    myAttributeDescriptor = attributesDescriptor;
  }

  @Nullable
  public JavaMethod getGetterMethod() {
    return myGetter;
  }

  @Nonnull
  public List<? extends DomElement> getValues(@Nonnull final DomInvocationHandler parent) {
    if (!parent.getGenericInfo().checkInitialized()) {
      return Collections.emptyList();
    }
    return parent.getCollectionChildren(this, CUSTOM_TAGS_GETTER);
  }

  @Nonnull
  public List<? extends DomElement> getValues(@Nonnull final DomElement parent) {
    final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(parent);
    if (handler != null) return getValues(handler);

    assert myGetter != null;
    return (List<? extends DomElement>)myGetter.invoke(parent, ArrayUtil.EMPTY_OBJECT_ARRAY);
  }

  public int compareTo(final AbstractDomChildDescriptionImpl o) {
    return equals(o) ? 0 : -1;
  }

  public List<XmlTag> getSubTags(final DomInvocationHandler handler, final XmlTag[] subTags, final XmlFile file) {
    return DomImplUtil.getCustomSubTags(handler, subTags, file);
  }

  public EvaluatedXmlName createEvaluatedXmlName(final DomInvocationHandler parent, final XmlTag childTag) {
    return new DummyEvaluatedXmlName(childTag.getLocalName(), childTag.getNamespace());
  }

  @Override
  public TagNameDescriptor getTagNameDescriptor() {
    return myTagNameDescriptor;
  }

  @Override
  public AttributeDescriptor getCustomAttributeDescriptor() {
    return myAttributeDescriptor;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CustomDomChildrenDescriptionImpl;
  }

  @Override
  public int hashCode() {
    return 239;
  }
}
