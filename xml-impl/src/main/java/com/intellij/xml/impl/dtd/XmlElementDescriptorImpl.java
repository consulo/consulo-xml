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
package com.intellij.xml.impl.dtd;

import consulo.xml.descriptor.XmlAttributeDescriptor;
import consulo.xml.descriptor.XmlElementDescriptor;
import consulo.xml.descriptor.XmlElementsGroup;
import consulo.xml.descriptor.XmlNSDescriptor;
import com.intellij.xml.util.XmlNSDescriptorSequence;
import com.intellij.xml.util.XmlUtil;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UserDataCache;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiWritableMetaData;
import consulo.language.psi.resolve.FilterElementProcessor;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.language.psi.*;

import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author Mike
 */
public class XmlElementDescriptorImpl extends BaseXmlElementDescriptorImpl implements PsiWritableMetaData {
    protected XmlElementDecl myElementDecl;
    private String myName;

    private static final Class[] ourParentClassesToScanAttributes = new Class[]{
        XmlMarkupDecl.class,
        XmlDocument.class
    };

    public XmlElementDescriptorImpl(XmlElementDecl elementDecl) {
        init(elementDecl);
    }

    public XmlElementDescriptorImpl() {
    }

    private static final UserDataCache<CachedValue<XmlAttlistDecl[]>, XmlElement, Object> ATTLIST_DECL_CACHE =
        new UserDataCache<CachedValue<XmlAttlistDecl[]>, XmlElement, Object>("cached_declarations") {
            @Override
            protected final CachedValue<XmlAttlistDecl[]> compute(XmlElement owner, Object o) {
                return CachedValuesManager.getManager(owner.getProject())
                    .createCachedValue(() -> new CachedValueProvider.Result<>(doCollectAttlistDeclarations(owner), owner));
            }
        };

    @Override
    public PsiElement getDeclaration() {
        return myElementDecl;
    }

    @Override
    public String getName(PsiElement context) {
        return getName();
    }

    @Override
    public String getName() {
        if (myName != null) {
            return myName;
        }
        return myName = myElementDecl.getName();
    }

    @Override
    public void init(PsiElement element) {
        myElementDecl = (XmlElementDecl)element;
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public Object[] getDependences() {
        return new Object[]{
            myElementDecl,
            ExternalResourceManager.getInstance()
        };
    }

    @Override
    public XmlNSDescriptor getNSDescriptor() {
        return getNsDescriptorFrom(myElementDecl);
    }

    @Nullable
    private static XmlNSDescriptor getNsDescriptorFrom(PsiElement elementDecl) {
        XmlFile file = XmlUtil.getContainingFile(elementDecl);
        if (file == null) {
            return null;
        }
        XmlDocument document = file.getDocument();
        assert document != null;
        XmlNSDescriptor descriptor = (XmlNSDescriptor)document.getMetaData();
        return descriptor == null ? document.getDefaultNSDescriptor(XmlUtil.EMPTY_URI, false) : descriptor;
    }

    // Read-only action
    @Override
    protected final XmlElementDescriptor[] doCollectXmlDescriptors(XmlTag context) {
        final LinkedHashSet<XmlElementDescriptor> result = new LinkedHashSet<>();
        XmlElementContentSpec contentSpecElement = myElementDecl.getContentSpecElement();
        XmlNSDescriptor nsDescriptor = getNSDescriptor();
        final XmlNSDescriptor NSDescriptor = nsDescriptor != null ? nsDescriptor : getNsDescriptorFrom(context);

        XmlUtil.processXmlElements(contentSpecElement, new PsiElementProcessor() {
            @Override
            public boolean execute(PsiElement child) {
                if (child instanceof XmlToken token) {
                    if (token.getTokenType() == XmlTokenType.XML_NAME) {
                        String text = child.getText();
                        XmlElementDescriptor element = getElementDescriptor(text, NSDescriptor);

                        if (element != null) {
                            result.add(element);
                        }
                    }
                    else if (token.getTokenType() == XmlTokenType.XML_CONTENT_ANY) {
                        if (NSDescriptor instanceof XmlNSDescriptorImpl xmlNSDescriptorImpl) {
                            ContainerUtil.addAll(result, xmlNSDescriptorImpl.getElements());
                        }
                        else if (NSDescriptor instanceof XmlNSDescriptorSequence xmlNSDescriptorSequence) {
                            for (XmlNSDescriptor xmlNSDescriptor : xmlNSDescriptorSequence.getSequence()) {
                                if (xmlNSDescriptor instanceof XmlNSDescriptorImpl xmlNSDescriptorImpl) {
                                    ContainerUtil.addAll(result, xmlNSDescriptorImpl.getElements());
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }, true, false, XmlUtil.getContainingFile(getDeclaration()));

        return result.toArray(new XmlElementDescriptor[result.size()]);
    }

    private static XmlElementDescriptor getElementDescriptor(String text, XmlNSDescriptor nsDescriptor) {
        XmlElementDescriptor element = null;
        if (nsDescriptor instanceof XmlNSDescriptorImpl xmlNSDescriptor) {
            element = xmlNSDescriptor.getElementDescriptor(text);
        }
        else if (nsDescriptor instanceof XmlNSDescriptorSequence xmlNSDescriptorSequence) {
            List<XmlNSDescriptor> sequence = xmlNSDescriptorSequence.getSequence();
            for (XmlNSDescriptor xmlNSDescriptor : sequence) {
                if (xmlNSDescriptor instanceof XmlNSDescriptorImpl xmlNSDescriptorImpl) {
                    element = xmlNSDescriptorImpl.getElementDescriptor(text);
                    if (element != null) {
                        break;
                    }
                }
            }
        }
        else {
            element = null;
        }
        return element;
    }

    // Read-only calculation
    @Override
    protected final XmlAttributeDescriptor[] collectAttributeDescriptors(XmlTag context) {
        List<XmlAttributeDescriptor> result = new SmartList<>();
        for (XmlAttlistDecl attlistDecl : findAttlistDeclarations(getName()))
            for (XmlAttributeDecl attributeDecl : attlistDecl.getAttributeDecls()) {
                PsiMetaData psiMetaData = attributeDecl.getMetaData();
                assert psiMetaData instanceof XmlAttributeDescriptor;
                result.add((XmlAttributeDescriptor) psiMetaData);
            }
        return result.toArray(new XmlAttributeDescriptor[result.size()]);
    }

    // Read-only calculation
    @Override
    protected Map<String, XmlAttributeDescriptor> collectAttributeDescriptorsMap(XmlTag context) {
        Map<String, XmlAttributeDescriptor> localADM;
        XmlAttributeDescriptor[] xmlAttributeDescriptors = getAttributesDescriptors(context);
        localADM = new HashMap<>(xmlAttributeDescriptors.length);

        for (XmlAttributeDescriptor xmlAttributeDescriptor : xmlAttributeDescriptors) {
            localADM.put(xmlAttributeDescriptor.getName(), xmlAttributeDescriptor);
        }
        return localADM;
    }

    private XmlAttlistDecl[] findAttlistDeclarations(String elementName) {
        List<XmlAttlistDecl> result = new ArrayList<>();
        for (XmlAttlistDecl declaration : getAttlistDeclarations()) {
            String name = declaration.getName();
            if (name != null && name.equals(elementName)) {
                result.add(declaration);
            }
        }
        return result.toArray(new XmlAttlistDecl[result.size()]);
    }

    private XmlAttlistDecl[] getAttlistDeclarations() {
        return getCachedAttributeDeclarations((XmlElement)getDeclaration());
    }

    public static
    XmlAttlistDecl[] getCachedAttributeDeclarations(@Nullable XmlElement owner) {
        if (owner == null) {
            return XmlAttlistDecl.EMPTY_ARRAY;
        }
        owner = (XmlElement)PsiTreeUtil.getParentOfType(owner, ourParentClassesToScanAttributes);
        if (owner == null) {
            return XmlAttlistDecl.EMPTY_ARRAY;
        }
        return ATTLIST_DECL_CACHE.get(owner, null).getValue();
    }

    private static XmlAttlistDecl[] doCollectAttlistDeclarations(XmlElement xmlElement) {
        final List<XmlAttlistDecl> result = new ArrayList<>();
        XmlUtil.processXmlElements(
            xmlElement,
            new FilterElementProcessor(new ClassFilter(XmlAttlistDecl.class), result),
            false,
            false,
            XmlUtil.getContainingFile(xmlElement)
        );
        return result.toArray(new XmlAttlistDecl[result.size()]);
    }

    @Override
    public XmlElementsGroup getTopGroup() {
        XmlElementContentGroup topGroup = myElementDecl.getContentSpecElement().getTopGroup();
        return topGroup == null ? null : new XmlElementsGroupImpl(topGroup, null);
    }

    @Override
    public int getContentType() {
        if (myElementDecl.getContentSpecElement().isAny()) {
            return CONTENT_TYPE_ANY;
        }
        if (myElementDecl.getContentSpecElement().hasChildren()) {
            return CONTENT_TYPE_CHILDREN;
        }
        if (myElementDecl.getContentSpecElement().isEmpty()) {
            return CONTENT_TYPE_EMPTY;
        }
        if (myElementDecl.getContentSpecElement().isMixed()) {
            return CONTENT_TYPE_MIXED;
        }

        return CONTENT_TYPE_ANY;
    }

    // Read-only calculation
    @Override
    protected Map<String, XmlElementDescriptor> collectElementDescriptorsMap(XmlTag element) {
        Map<String, XmlElementDescriptor> elementDescriptorsMap;
        XmlElementDescriptor[] descriptors = getElementsDescriptors(element);
        elementDescriptorsMap = new HashMap<>(descriptors.length);

        for (XmlElementDescriptor descriptor : descriptors) {
            elementDescriptorsMap.put(descriptor.getName(), descriptor);
        }
        return elementDescriptorsMap;
    }

    @Override
    public String getQualifiedName() {
        return getName();
    }

    @Override
    public String getDefaultName() {
        return getName();
    }

    @Override
    public void setName(String name) throws IncorrectOperationException {
        // IDEADEV-11439
        myName = null;
    }
}