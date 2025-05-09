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

import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.application.util.function.Computable;
import consulo.application.util.function.Processor;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SoftReference;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.GenericDomValue;
import consulo.xml.util.xml.JavaMethod;
import consulo.xml.util.xml.reflect.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author peter
 */
public class DynamicGenericInfo extends DomGenericInfoEx {
  private static final Key<SoftReference<ConcurrentHashMap<ChildrenDescriptionsHolder, ChildrenDescriptionsHolder>>> HOLDERS_CACHE = Key.create("DOM_CHILDREN_HOLDERS_CACHE");
  private static final RecursionGuard ourGuard = RecursionManager.createGuard("dynamicGenericInfo");
  private final StaticGenericInfo myStaticGenericInfo;
  @Nonnull
  private final DomInvocationHandler myInvocationHandler;
  private volatile boolean myInitialized;
  private volatile ChildrenDescriptionsHolder<AttributeChildDescriptionImpl> myAttributes;
  private volatile ChildrenDescriptionsHolder<FixedChildDescriptionImpl> myFixeds;
  private volatile ChildrenDescriptionsHolder<CollectionChildDescriptionImpl> myCollections;
  private volatile List<CustomDomChildrenDescriptionImpl> myCustomChildren;

  public DynamicGenericInfo(@Nonnull final DomInvocationHandler handler, final StaticGenericInfo staticGenericInfo) {
    myInvocationHandler = handler;
    myStaticGenericInfo = staticGenericInfo;

    myAttributes = staticGenericInfo.getAttributes();
    myFixeds = staticGenericInfo.getFixed();
    myCollections = staticGenericInfo.getCollections();
  }

  public Invocation createInvocation(final JavaMethod method) {
    return myStaticGenericInfo.createInvocation(method);
  }

  public final boolean checkInitialized() {
    if (myInitialized) return true;
    myStaticGenericInfo.buildMethodMaps();

    if (!myInvocationHandler.exists()) return true;

    return ourGuard.doPreventingRecursion(myInvocationHandler, false, new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        DomExtensionsRegistrarImpl registrar = runDomExtenders();

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (myInvocationHandler) {
          if (!myInitialized) {
            if (registrar != null) {
              applyExtensions(registrar);
            }
            myInitialized = true;
          }
        }
        return Boolean.TRUE;
      }
    }) == Boolean.TRUE;
  }

  private void applyExtensions(DomExtensionsRegistrarImpl registrar) {
    XmlFile file = myInvocationHandler.getFile();

    final List<DomExtensionImpl> fixeds = registrar.getFixeds();
    final List<DomExtensionImpl> collections = registrar.getCollections();
    final List<DomExtensionImpl> attributes = registrar.getAttributes();
    if (!attributes.isEmpty()) {
      ChildrenDescriptionsHolder<AttributeChildDescriptionImpl> newAttributes = new ChildrenDescriptionsHolder<AttributeChildDescriptionImpl>(myStaticGenericInfo.getAttributes());
      for (final DomExtensionImpl extension : attributes) {
        newAttributes.addDescription(extension.addAnnotations(new AttributeChildDescriptionImpl(extension.getXmlName(), extension.getType())));
      }
      myAttributes = internChildrenHolder(file, newAttributes);
    }

    if (!fixeds.isEmpty()) {
      ChildrenDescriptionsHolder<FixedChildDescriptionImpl> newFixeds = new ChildrenDescriptionsHolder<FixedChildDescriptionImpl>(myStaticGenericInfo.getFixed());
      for (final DomExtensionImpl extension : fixeds) {
        //noinspection unchecked
        newFixeds.addDescription(extension.addAnnotations(new FixedChildDescriptionImpl(extension.getXmlName(), extension.getType(), extension.getCount(), ArrayUtil.EMPTY_COLLECTION_ARRAY)));
      }
      myFixeds = internChildrenHolder(file, newFixeds);
    }
    if (!collections.isEmpty()) {
      ChildrenDescriptionsHolder<CollectionChildDescriptionImpl> newCollections = new ChildrenDescriptionsHolder<CollectionChildDescriptionImpl>(myStaticGenericInfo.getCollections());
      for (final DomExtensionImpl extension : collections) {
        newCollections.addDescription(extension.addAnnotations(new CollectionChildDescriptionImpl(extension.getXmlName(), extension.getType(),
            Collections.<JavaMethod>emptyList()
        )));
      }
      myCollections = internChildrenHolder(file, newCollections);
    }

    final List<DomExtensionImpl> customs = registrar.getCustoms();
    myCustomChildren = customs.isEmpty() ? null : ContainerUtil.map(customs, extension -> new CustomDomChildrenDescriptionImpl(extension));
  }

  private static <T extends DomChildDescriptionImpl> ChildrenDescriptionsHolder<T> internChildrenHolder(XmlFile file, ChildrenDescriptionsHolder<T> holder) {
    SoftReference<ConcurrentHashMap<ChildrenDescriptionsHolder, ChildrenDescriptionsHolder>> ref = file.getUserData(HOLDERS_CACHE);
    ConcurrentHashMap<ChildrenDescriptionsHolder, ChildrenDescriptionsHolder> cache = ref == null ? null : ref.get();
    if (cache == null) {
      cache = new ConcurrentHashMap<ChildrenDescriptionsHolder, ChildrenDescriptionsHolder>();
      file.putUserData(HOLDERS_CACHE, new SoftReference<ConcurrentHashMap<ChildrenDescriptionsHolder, ChildrenDescriptionsHolder>>(cache));
    }
    ChildrenDescriptionsHolder existing = cache.get(holder);
    if (existing != null) {
      //noinspection unchecked
      return existing;
    }
    cache.put(holder, holder);
    return holder;
  }

  @Nullable
  private DomExtensionsRegistrarImpl runDomExtenders() {
    final DomExtensionsRegistrarImpl registrar = new DomExtensionsRegistrarImpl();
    final DomElement domElement = myInvocationHandler.getProxy();
    final Project project = myInvocationHandler.getManager().getProject();
    project.getApplication().getExtensionPoint(DomExtender.class).forEachExtensionSafe(extender -> register(extender, domElement, registrar));

    final AbstractDomChildDescriptionImpl description = myInvocationHandler.getChildDescription();
    if (description != null) {
      final List<DomExtender> extenders = description.getUserData(DomExtensionImpl.DOM_EXTENDER_KEY);
      if (extenders != null) {
        for (final DomExtender extender : extenders) {
          register(extender, domElement, registrar);
        }
      }
    }
    return registrar.isEmpty() ? null : registrar;
  }

  @SuppressWarnings("unchecked")
  private static void register(DomExtender extender, DomElement domElement, DomExtensionsRegistrarImpl registrar) {
    Class elementClass = extender.getElementClass();

    if (elementClass.isInstance(domElement)) {
      extender.registerExtensions(domElement, registrar);
    }
  }

  public XmlElement getNameElement(DomElement element) {
    return myStaticGenericInfo.getNameElement(element);
  }

  public GenericDomValue getNameDomElement(DomElement element) {
    return myStaticGenericInfo.getNameDomElement(element);
  }

  @Nonnull
  public List<? extends CustomDomChildrenDescription> getCustomNameChildrenDescription() {
    checkInitialized();
    if (myCustomChildren != null) return myCustomChildren;
    return myStaticGenericInfo.getCustomNameChildrenDescription();
  }

  public String getElementName(DomElement element) {
    return myStaticGenericInfo.getElementName(element);
  }

  @Nonnull
  public List<AbstractDomChildDescriptionImpl> getChildrenDescriptions() {
    checkInitialized();
    final ArrayList<AbstractDomChildDescriptionImpl> list = new ArrayList<AbstractDomChildDescriptionImpl>();
    myAttributes.dumpDescriptions(list);
    myFixeds.dumpDescriptions(list);
    myCollections.dumpDescriptions(list);
    list.addAll(myStaticGenericInfo.getCustomNameChildrenDescription());
    return list;
  }

  @Nonnull
  public final List<FixedChildDescriptionImpl> getFixedChildrenDescriptions() {
    checkInitialized();
    return myFixeds.getDescriptions();
  }

  @Nonnull
  public final List<CollectionChildDescriptionImpl> getCollectionChildrenDescriptions() {
    checkInitialized();
    return myCollections.getDescriptions();
  }

  public FixedChildDescriptionImpl getFixedChildDescription(String tagName) {
    checkInitialized();
    return myFixeds.findDescription(tagName);
  }

  public DomFixedChildDescription getFixedChildDescription(@NonNls String tagName, @NonNls String namespace) {
    checkInitialized();
    return myFixeds.getDescription(tagName, namespace);
  }

  public CollectionChildDescriptionImpl getCollectionChildDescription(String tagName) {
    checkInitialized();
    return myCollections.findDescription(tagName);
  }

  public DomCollectionChildDescription getCollectionChildDescription(@NonNls String tagName, @NonNls String namespace) {
    checkInitialized();
    return myCollections.getDescription(tagName, namespace);
  }

  public AttributeChildDescriptionImpl getAttributeChildDescription(String attributeName) {
    checkInitialized();
    return myAttributes.findDescription(attributeName);
  }


  public DomAttributeChildDescription getAttributeChildDescription(@NonNls String attributeName, @NonNls String namespace) {
    checkInitialized();
    return myAttributes.getDescription(attributeName, namespace);
  }

  public boolean isTagValueElement() {
    return myStaticGenericInfo.isTagValueElement();
  }

  @Nonnull
  public List<AttributeChildDescriptionImpl> getAttributeChildrenDescriptions() {
    checkInitialized();
    return myAttributes.getDescriptions();
  }

  @Override
  public boolean processAttributeChildrenDescriptions(final Processor<AttributeChildDescriptionImpl> processor) {
    final Set<AttributeChildDescriptionImpl> visited = new HashSet<AttributeChildDescriptionImpl>();
    if (!myStaticGenericInfo.processAttributeChildrenDescriptions(new Processor<AttributeChildDescriptionImpl>() {
      public boolean process(AttributeChildDescriptionImpl attributeChildDescription) {
        visited.add(attributeChildDescription);
        return processor.process(attributeChildDescription);
      }
    })) {
      return false;
    }
    for (final AttributeChildDescriptionImpl description : getAttributeChildrenDescriptions()) {
      if (!visited.contains(description) && !processor.process(description)) {
        return false;
      }
    }
    return true;
  }

}
