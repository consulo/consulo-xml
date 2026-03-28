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

import java.util.Map;

import consulo.application.util.FieldCache;
import consulo.application.util.SimpleFieldCache;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;

public abstract class BaseXmlElementDescriptorImpl implements XmlElementDescriptor {
    private volatile XmlElementDescriptor[] myElementDescriptors;
    private volatile XmlAttributeDescriptor[] myAttributeDescriptors;
    private volatile Map<String, XmlElementDescriptor> myElementDescriptorsMap;
    private volatile Map<String, XmlAttributeDescriptor> attributeDescriptorsMap;

    protected BaseXmlElementDescriptorImpl() {
    }

    @Override
    public XmlElementsGroup getTopGroup() {
        return null;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    // Read-only action
    protected abstract XmlElementDescriptor[] doCollectXmlDescriptors(XmlTag context);

    static final FieldCache<XmlElementDescriptor[], BaseXmlElementDescriptorImpl, Object, XmlTag> myElementDescriptorsCache =
        new FieldCache<>() {
            @Override
            protected final XmlElementDescriptor[] compute(BaseXmlElementDescriptorImpl xmlElementDescriptor, XmlTag tag) {
                return xmlElementDescriptor.doCollectXmlDescriptors(tag);
            }

            @Override
            protected final XmlElementDescriptor[] getValue(BaseXmlElementDescriptorImpl xmlElementDescriptor, Object o) {
                return xmlElementDescriptor.myElementDescriptors;
            }

            @Override
            protected final void putValue(
                XmlElementDescriptor[] xmlElementDescriptors,
                BaseXmlElementDescriptorImpl xmlElementDescriptor,
                Object o
            ) {
                xmlElementDescriptor.myElementDescriptors = xmlElementDescriptors;
            }
        };

    @Override
    public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
        return myElementDescriptorsCache.get(null, this, context);
    }

    private static final SimpleFieldCache<XmlAttributeDescriptor[], BaseXmlElementDescriptorImpl> myAttributeDescriptorsCache =
        new SimpleFieldCache<>() {
            @Override
            protected final XmlAttributeDescriptor[] compute(BaseXmlElementDescriptorImpl xmlElementDescriptor) {
                return xmlElementDescriptor.collectAttributeDescriptors(null);
            }

            @Override
            protected final XmlAttributeDescriptor[] getValue(BaseXmlElementDescriptorImpl xmlElementDescriptor) {
                return xmlElementDescriptor.myAttributeDescriptors;
            }

            @Override
            protected final void putValue(
                XmlAttributeDescriptor[] xmlAttributeDescriptors,
                BaseXmlElementDescriptorImpl xmlElementDescriptor
            ) {
                xmlElementDescriptor.myAttributeDescriptors = xmlAttributeDescriptors;
            }
        };

    @Override
    public XmlAttributeDescriptor[] getAttributesDescriptors(XmlTag context) {
        return myAttributeDescriptorsCache.get(this);
    }

    // Read-only calculation
    protected abstract XmlAttributeDescriptor[] collectAttributeDescriptors(XmlTag context);

    private static final SimpleFieldCache<Map<String, XmlAttributeDescriptor>, BaseXmlElementDescriptorImpl>
        attributeDescriptorsMapCache = new SimpleFieldCache<>() {
        @Override
        protected final Map<String, XmlAttributeDescriptor> compute(BaseXmlElementDescriptorImpl baseXmlElementDescriptor) {
            return baseXmlElementDescriptor.collectAttributeDescriptorsMap(null);
        }

        @Override
        protected final Map<String, XmlAttributeDescriptor> getValue(BaseXmlElementDescriptorImpl baseXmlElementDescriptor) {
            return baseXmlElementDescriptor.attributeDescriptorsMap;
        }

        @Override
        protected final void putValue(Map<String, XmlAttributeDescriptor> hashMap, BaseXmlElementDescriptorImpl baseXmlElementDescriptor) {
            baseXmlElementDescriptor.attributeDescriptorsMap = hashMap;
        }
    };

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, XmlTag context) {
        return attributeDescriptorsMapCache.get(this).get(attributeName);
    }

    // Read-only calculation
    protected abstract Map<String, XmlAttributeDescriptor> collectAttributeDescriptorsMap(XmlTag context);

    private static final FieldCache<Map<String, XmlElementDescriptor>, BaseXmlElementDescriptorImpl, Object, XmlTag>
        myElementDescriptorsMapCache = new FieldCache<>() {
        @Override
        protected final Map<String, XmlElementDescriptor> compute(BaseXmlElementDescriptorImpl baseXmlElementDescriptor, XmlTag p) {
            return baseXmlElementDescriptor.collectElementDescriptorsMap(p);
        }

        @Override
        protected final Map<String, XmlElementDescriptor> getValue(BaseXmlElementDescriptorImpl baseXmlElementDescriptor, Object p) {
            return baseXmlElementDescriptor.myElementDescriptorsMap;
        }

        @Override
        protected final void putValue(
            Map<String, XmlElementDescriptor> hashMap,
            BaseXmlElementDescriptorImpl baseXmlElementDescriptor,
            Object p
        ) {
            baseXmlElementDescriptor.myElementDescriptorsMap = hashMap;
        }
    };

    @Override
    public XmlElementDescriptor getElementDescriptor(XmlTag element, XmlTag contextTag) {
        return myElementDescriptorsMapCache.get(null, this, element).get(element.getName());
    }

    public final XmlElementDescriptor getElementDescriptor(String name, XmlTag context) {
        return myElementDescriptorsMapCache.get(null, this, context).get(name);
    }

    // Read-only calculation
    protected abstract Map<String, XmlElementDescriptor> collectElementDescriptorsMap(XmlTag element);

    @Override
    public final XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attr) {
        return getAttributeDescriptor(attr.getName(), attr.getParent());
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }
}
