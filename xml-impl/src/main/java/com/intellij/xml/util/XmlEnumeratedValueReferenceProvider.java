/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.xml.impl.XmlEnumerationDescriptor;
import com.intellij.xml.impl.schema.XmlSchemaTagsProcessor;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;

import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * Date: 15.08.13
 */
public class XmlEnumeratedValueReferenceProvider<T extends PsiElement> extends PsiReferenceProvider {
    public final static Key<Boolean> SUPPRESS = Key.create("suppress attribute value references");

    @Nonnull
    @Override
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        if (XmlSchemaTagsProcessor.PROCESSING_FLAG.get() != null || context.get(SUPPRESS) != null) {
            return PsiReference.EMPTY_ARRAY;
        }
        @SuppressWarnings("unchecked") PsiElement host = getHost((T)element);
        if (host instanceof PsiLanguageInjectionHost
            && InjectedLanguageManager.getInstance(element.getProject()).getInjectedPsiFiles(host) != null) {
            return PsiReference.EMPTY_ARRAY;
        }
        String unquotedValue = ElementManipulators.getValueText(element);
        if (XmlHighlightVisitor.skipValidation(element) || !XmlUtil.isSimpleValue(unquotedValue, element)) {
            return PsiReference.EMPTY_ARRAY;
        }
        @SuppressWarnings("unchecked") final Object descriptor = getDescriptor((T)element);
        if (descriptor instanceof XmlEnumerationDescriptor enumerationDescriptor) {
            XmlElement xmlElement = (XmlElement)element;
            if (enumerationDescriptor.isFixed() || enumerationDescriptor.isEnumerated(xmlElement)) {
                //noinspection unchecked
                return enumerationDescriptor.getValueReferences(xmlElement, unquotedValue);
            }
            else if (unquotedValue.equals(enumerationDescriptor.getDefaultValue())) {  // todo case insensitive
                return ContainerUtil.map2Array(
                    enumerationDescriptor.getValueReferences(xmlElement, unquotedValue),
                    PsiReference.class,
                    reference -> PsiDelegateReference.createSoft(reference, true)
                );
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    protected PsiElement getHost(T element) {
        return element;
    }

    protected Object getDescriptor(T element) {
        PsiElement parent = element.getParent();
        return parent instanceof XmlAttribute ? ((XmlAttribute)parent).getDescriptor() : null;
    }

    public static XmlEnumeratedValueReferenceProvider forTags() {
        return new XmlEnumeratedValueReferenceProvider<XmlTag>() {
            @Override
            protected Object getDescriptor(XmlTag element) {
                return element.getDescriptor();
            }

            @Override
            protected PsiElement getHost(XmlTag element) {
                XmlText[] textElements = element.getValue().getTextElements();
                return ArrayUtil.getFirstElement(textElements);
            }
        };
    }
}
