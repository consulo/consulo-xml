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
package com.intellij.xml.impl.schema;

import consulo.language.psi.PsiElement;
import org.jetbrains.annotations.NonNls;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.util.XmlUtil;

/**
 * @author ik
 */
public class XmlElementDescriptorByType extends XmlElementDescriptorImpl {
    private ComplexTypeDescriptor myType;
    @NonNls
    public static final String QUALIFIED_ATTR_VALUE = "qualified";

    public XmlElementDescriptorByType(XmlTag instanceTag, ComplexTypeDescriptor descriptor) {
        myDescriptorTag = instanceTag;
        myType = descriptor;
    }

    public XmlElementDescriptorByType() {
    }

    @Override
    public XmlTag getDeclaration() {
        return myDescriptorTag;
    }

    @Override
    public String getName(PsiElement context) {
        return myDescriptorTag.getName();
    }

    @Override
    public XmlNSDescriptor getNSDescriptor() {
        XmlNSDescriptor nsDescriptor = NSDescriptor;
        if (nsDescriptor == null) {
            final XmlFile file = XmlUtil.getContainingFile(getType(null).getDeclaration());
            if (file == null) {
                return null;
            }
            final XmlDocument document = file.getDocument();
            if (document == null) {
                return null;
            }
            NSDescriptor = nsDescriptor = (XmlNSDescriptor)document.getMetaData();
        }

        return nsDescriptor;
    }

    @Override
    public ComplexTypeDescriptor getType(XmlElement context) {
        return myType;
    }

    @Override
    public String getDefaultName() {
        XmlTag rootTag = ((XmlFile)getType(null).getDeclaration().getContainingFile()).getDocument().getRootTag();

        if (QUALIFIED_ATTR_VALUE.equals(rootTag.getAttributeValue("elementFormDefault"))) {
            return getQualifiedName();
        }

        return getName();
    }

    @Override
    protected boolean askParentDescriptorViaXsi() {
        return false;
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XmlElementDescriptorByType)) {
            return false;
        }

        final XmlElementDescriptorByType that = (XmlElementDescriptorByType)o;

        if (myType != null ? !myType.equals(that.myType) : that.myType != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (myType != null ? myType.hashCode() : 0);
    }
}
