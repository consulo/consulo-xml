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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.RecursionManager;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.sem.SemContributor;
import consulo.language.sem.SemRegistrar;
import consulo.language.sem.SemService;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.EvaluatedXmlName;
import consulo.xml.util.xml.EvaluatedXmlNameImpl;
import consulo.xml.util.xml.XmlName;
import consulo.xml.util.xml.reflect.CustomDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomChildrenDescription;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.reflect.DomFixedChildDescription;
import consulo.xml.util.xml.stubs.DomStub;
import consulo.xml.util.xml.stubs.ElementStub;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static consulo.xml.patterns.XmlPatterns.*;

/**
 * @author peter
 */
@ExtensionImpl
public class DomSemContributor extends SemContributor {
  private final SemService mySemService;

  @Inject
  public DomSemContributor(SemService semService) {
    mySemService = semService;
  }

  @Override
  public void registerSemProviders(SemRegistrar registrar) {
    registrar.registerSemElementProvider(DomManagerImpl.FILE_DESCRIPTION_KEY, xmlFile(), xmlFile -> {
      ApplicationManager.getApplication().assertReadAccessAllowed();
      return new FileDescriptionCachedValueProvider(DomManagerImpl.getDomManager(xmlFile.getProject()), xmlFile);
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_HANDLER_KEY, xmlTag().withParent(psiElement(XmlElementType.XML_DOCUMENT).withParent(xmlFile())), xmlTag -> {
      final FileDescriptionCachedValueProvider provider =
        mySemService.getSemElement(DomManagerImpl.FILE_DESCRIPTION_KEY, xmlTag.getContainingFile());
      assert provider != null;
      final DomFileElementImpl element = provider.getFileElement();
      if (element != null) {
        final DomRootInvocationHandler handler = element.getRootHandler();
        if (handler.getXmlTag() == xmlTag) {
          return handler;
        }
      }
      return null;
    });

    final ElementPattern<XmlTag> nonRootTag = xmlTag().withParent(or(xmlTag(), xmlEntityRef().withParent(xmlTag())));
    registrar.registerSemElementProvider(DomManagerImpl.DOM_INDEXED_HANDLER_KEY, nonRootTag, tag -> {
      final XmlTag parentTag = PhysicalDomParentStrategy.getParentTag(tag);
      assert parentTag != null;
      DomInvocationHandler parent = getParentDom(parentTag);
      if (parent == null) return null;

      final String localName = tag.getLocalName();
      final String namespace = tag.getNamespace();

      final DomFixedChildDescription description =
        findChildrenDescription(parent.getGenericInfo().getFixedChildrenDescriptions(), tag, parent);

      if (description != null) {

        final int totalCount = description.getCount();

        int index = 0;
        PsiElement current = tag;
        while (true) {
          current = current.getPrevSibling();
          if (current == null) {
            break;
          }
          if (current instanceof XmlTag) {
            final XmlTag xmlTag = (XmlTag)current;
            if (localName.equals(xmlTag.getLocalName()) && namespace.equals(xmlTag.getNamespace())) {
              index++;
              if (index >= totalCount) {
                return null;
              }
            }
          }
        }

        final DomManagerImpl myDomManager = parent.getManager();
        return new IndexedElementInvocationHandler(parent.createEvaluatedXmlName(description.getXmlName()), (FixedChildDescriptionImpl)description, index,
                                            new PhysicalDomParentStrategy(tag, myDomManager), myDomManager, null);
      }
      return null;
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_COLLECTION_HANDLER_KEY, nonRootTag, tag -> {
      final XmlTag parentTag = PhysicalDomParentStrategy.getParentTag(tag);
      assert parentTag != null;
      DomInvocationHandler parent = getParentDom(parentTag);
      if (parent == null) return null;

      final DomCollectionChildDescription description = findChildrenDescription(parent.getGenericInfo().getCollectionChildrenDescriptions(), tag, parent);
      if (description != null) {
        DomStub parentStub = parent.getStub();
        if (parentStub != null) {
          int index = ArrayUtil.indexOf(parentTag.findSubTags(tag.getName(), tag.getNamespace()), tag);
          ElementStub stub = parentStub.getElementStub(tag.getLocalName(), index);
          if (stub != null) {
            XmlName name = description.getXmlName();
            EvaluatedXmlNameImpl evaluatedXmlName = EvaluatedXmlNameImpl.createEvaluatedXmlName(name, name.getNamespaceKey(), true);
            return new CollectionElementInvocationHandler(evaluatedXmlName, (AbstractDomChildDescriptionImpl)description, parent.getManager(), stub);
          }
        }
        return new CollectionElementInvocationHandler(description.getType(), tag, (AbstractCollectionChildDescription)description, parent, null);
      }
      return null;
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_CUSTOM_HANDLER_KEY, nonRootTag, tag -> {
      if (StringUtil.isEmpty(tag.getName())) return null;

      final XmlTag parentTag = PhysicalDomParentStrategy.getParentTag(tag);
      assert parentTag != null;

      DomInvocationHandler parent = RecursionManager.doPreventingRecursion(tag, true, () -> getParentDom(parentTag));
      if (parent == null) return null;

      DomGenericInfoEx info = parent.getGenericInfo();
      final List<? extends CustomDomChildrenDescription> customs = info.getCustomNameChildrenDescription();
      if (customs.isEmpty()) return null;

      if (mySemService.getSemElement(DomManagerImpl.DOM_INDEXED_HANDLER_KEY, tag) == null &&
          mySemService.getSemElement(DomManagerImpl.DOM_COLLECTION_HANDLER_KEY, tag) == null) {

        String localName = tag.getLocalName();
        XmlFile file = parent.getFile();
        for (final DomFixedChildDescription description : info.getFixedChildrenDescriptions()) {
          XmlName xmlName = description.getXmlName();
          if (localName.equals(xmlName.getLocalName()) && DomImplUtil.isNameSuitable(xmlName, tag, parent, file)) {
            return null;
          }
        }
        for (CustomDomChildrenDescription description : customs) {
          if (description.getTagNameDescriptor() != null) {
           AbstractCollectionChildDescription desc = (AbstractCollectionChildDescription)description;
           Type type = description.getType();
           return new CollectionElementInvocationHandler(type, tag, desc, parent, null);
          }
        }
      }

      return null;
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_ATTRIBUTE_HANDLER_KEY, xmlAttribute(), attribute -> {
      final XmlTag tag = PhysicalDomParentStrategy.getParentTag(attribute);
      final DomInvocationHandler handler = getParentDom(tag);
      if (handler == null) return null;

      final String localName = attribute.getLocalName();
      final Ref<AttributeChildInvocationHandler> result = Ref.create(null);
      handler.getGenericInfo().processAttributeChildrenDescriptions(description -> {
        if (description.getXmlName().getLocalName().equals(localName)) {
          final EvaluatedXmlName evaluatedXmlName = handler.createEvaluatedXmlName(description.getXmlName());

          final String ns = evaluatedXmlName.getNamespace(tag, handler.getFile());
          //see XmlTagImpl.getAttribute(localName, namespace)
          if (ns.equals(tag.getNamespace()) && localName.equals(attribute.getName()) ||
              ns.equals(attribute.getNamespace())) {
            final DomManagerImpl myDomManager = handler.getManager();
            final AttributeChildInvocationHandler attributeHandler =
              new AttributeChildInvocationHandler(evaluatedXmlName, description, myDomManager,
                                                  new PhysicalDomParentStrategy(attribute, myDomManager), null);
            result.set(attributeHandler);
            return false;
          }
        }
        return true;
      });

      return result.get();
    });

  }

  @Nullable
  private static DomInvocationHandler getParentDom(@Nonnull XmlTag tag) {
    LinkedHashSet<XmlTag> allParents = new LinkedHashSet<>();
    PsiElement each = tag;
    while (each instanceof XmlTag && allParents.add((XmlTag)each)) {
      each = PhysicalDomParentStrategy.getParentTagCandidate((XmlTag)each);
    }
    ArrayList<XmlTag> list = new ArrayList<>(allParents);
    Collections.reverse(list);
    DomManagerImpl manager = DomManagerImpl.getDomManager(tag.getProject());
    for (XmlTag xmlTag : list) {
      manager.getDomHandler(xmlTag);
    }

    return manager.getDomHandler(tag);
  }

  @Nullable
  private static <T extends DomChildrenDescription> T findChildrenDescription(List<T> descriptions, XmlTag tag, DomInvocationHandler parent) {
    final String localName = tag.getLocalName();
    String namespace = null;
    final String qName = tag.getName();

    final XmlFile file = parent.getFile();

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, size = descriptions.size(); i < size; i++) {
      final T description = descriptions.get(i);
      final XmlName xmlName = description.getXmlName();

      if (localName.equals(xmlName.getLocalName()) || qName.equals(xmlName.getLocalName())) {
        final EvaluatedXmlName evaluatedXmlName = parent.createEvaluatedXmlName(xmlName);
        if (DomImplUtil.isNameSuitable(evaluatedXmlName,
                                       localName,
                                       qName,
                                       namespace == null ? namespace = tag.getNamespace() : namespace,
                                       file)) {
          return description;
        }
      }
    }
    return null;
  }
}
