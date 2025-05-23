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
package com.intellij.xml.impl.dom;

import jakarta.annotation.Nonnull;

import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.XmlName;
import consulo.xml.util.xml.impl.DomInvocationHandler;
import consulo.xml.util.xml.impl.DomManagerImpl;
import consulo.xml.util.xml.reflect.DomAttributeChildDescription;
import com.intellij.xml.NamespaceAwareXmlAttributeDescriptor;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;

/**
 * @author mike
 */
public class DomAttributeXmlDescriptor implements NamespaceAwareXmlAttributeDescriptor {
    private final DomAttributeChildDescription myDescription;
    private final Project myProject;

    public DomAttributeXmlDescriptor(final DomAttributeChildDescription description, Project project) {
        myDescription = description;
        myProject = project;
    }

    public boolean isRequired() {
        return false;
    }

    public boolean isFixed() {
        return false;
    }

    public boolean hasIdType() {
        return false;
    }

    public boolean hasIdRefType() {
        return false;
    }

    @Nullable
    public String getDefaultValue() {
        return null;
    }//todo: refactor to hierarchy of value descriptor?

    public boolean isEnumerated() {
        return false;
    }

    @Nullable
    public String[] getEnumeratedValues() {
        return null;
    }

    @Nullable
    public String validateValue(final XmlElement context, final String value) {
        return null;
    }

    @Nullable
    public PsiElement getDeclaration() {
        return myDescription.getDeclaration(myProject);
    }

    @NonNls
    public String getName(final PsiElement context) {
        return getQualifiedAttributeName(context, myDescription.getXmlName());
    }

    static String getQualifiedAttributeName(PsiElement context, XmlName xmlName) {
        final String localName = xmlName.getLocalName();
        if (context instanceof XmlTag tag) {
            final DomInvocationHandler handler = DomManagerImpl.getDomManager(context.getProject()).getDomHandler(tag);
            if (handler != null) {
                final String ns = handler.createEvaluatedXmlName(xmlName).getNamespace(tag, handler.getFile());
                if (!ns.equals(XmlUtil.EMPTY_URI) && !ns.equals(tag.getNamespace())) {
                    final String prefix = tag.getPrefixByNamespace(ns);
                    if (StringUtil.isNotEmpty(prefix)) {
                        return prefix + ":" + localName;
                    }
                }
            }
        }

        return localName;
    }

    @NonNls
    public String getName() {
        return getLocalName();
    }

    private String getLocalName() {
        return myDescription.getXmlName().getLocalName();
    }

    @Nullable
    public String getNamespace(@Nonnull XmlTag context) {
        final DomInvocationHandler handler = DomManagerImpl.getDomManager(myProject).getDomHandler(context);

        if (handler == null) {
            return null;
        }
        return handler.createEvaluatedXmlName(myDescription.getXmlName()).getNamespace(context, handler.getFile());
    }

    public void init(final PsiElement element) {
        throw new UnsupportedOperationException("Method init not implemented in " + getClass());
    }

    public Object[] getDependences() {
        throw new UnsupportedOperationException("Method getDependences not implemented in " + getClass());
    }
}
