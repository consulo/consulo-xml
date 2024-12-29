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
package com.intellij.xml.util;

import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

/**
 * @author mike
 */
public class XmlIncludeHandler {
    @NonNls
    private static final String INCLUDE_TAG_NAME = "include";

    public static boolean isXInclude(PsiElement element) {
        if (element instanceof XmlTag xmlTag) {
            if (xmlTag.getParent() instanceof XmlDocument) {
                return false;
            }

            if (INCLUDE_TAG_NAME.equals(xmlTag.getLocalName())
                && xmlTag.getAttributeValue("href") != null
                && XmlUtil.XINCLUDE_URI.equals(xmlTag.getNamespace())) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public static XmlFile resolveXIncludeFile(XmlTag xincludeTag) {
        final XmlAttribute hrefAttribute = xincludeTag.getAttribute("href", null);
        if (hrefAttribute == null) {
            return null;
        }

        final XmlAttributeValue xmlAttributeValue = hrefAttribute.getValueElement();
        if (xmlAttributeValue == null) {
            return null;
        }

        final FileReferenceSet referenceSet = FileReferenceSet.createSet(xmlAttributeValue, false, true, false);

        final PsiReference reference = referenceSet.getLastReference();
        if (reference == null) {
            return null;
        }

        final PsiElement target = reference.resolve();

        return target instanceof XmlFile file ? file : null;
    }
}
