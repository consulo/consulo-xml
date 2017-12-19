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
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

/**
 * @author mike
 */
public class XmlIncludeHandler {
  @NonNls private static final String INCLUDE_TAG_NAME = "include";
  public static boolean isXInclude(PsiElement element) {
    if (element instanceof XmlTag) {
      XmlTag xmlTag = (XmlTag)element;

      if (xmlTag.getParent() instanceof XmlDocument) return false;

      if (xmlTag.getLocalName().equals(INCLUDE_TAG_NAME) && xmlTag.getAttributeValue("href") != null) {
        if (xmlTag.getNamespace().equals(XmlUtil.XINCLUDE_URI)) {
          return true;
        }
      }
    }

    return false;
  }

  @Nullable
  public static XmlFile resolveXIncludeFile(XmlTag xincludeTag) {
    final XmlAttribute hrefAttribute = xincludeTag.getAttribute("href", null);
    if (hrefAttribute == null) return null;

    final XmlAttributeValue xmlAttributeValue = hrefAttribute.getValueElement();
    if (xmlAttributeValue == null) return null;

    final FileReferenceSet referenceSet = FileReferenceSet.createSet(xmlAttributeValue, false, true, false);

    final PsiReference reference = referenceSet.getLastReference();
    if (reference == null) return null;

    final PsiElement target = reference.resolve();

    if (!(target instanceof XmlFile)) return null;
    return (XmlFile)target;
  }
}