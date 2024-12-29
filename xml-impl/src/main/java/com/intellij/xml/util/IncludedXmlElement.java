/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.language.impl.psi.LightElement;
import consulo.language.impl.psi.PsiAnchor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.util.lang.ref.SoftReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class IncludedXmlElement<T extends XmlElement> extends LightElement implements XmlElement {
    private final PsiAnchor myOriginal;
    private SoftReference<T> myRef;
    private final PsiElement myParent;

    public IncludedXmlElement(@Nonnull T original, @Nullable PsiElement parent) {
        super(original.getManager(), original.getLanguage());
        //noinspection unchecked
        T realOriginal = original instanceof IncludedXmlElement ? ((IncludedXmlElement<T>)original).getOriginal() : original;
        myOriginal = PsiAnchor.create(realOriginal);
        myRef = new SoftReference<>(realOriginal);
        myParent = parent;
    }

    @Override
    public boolean isValid() {
        T t = myRef.get();
        if (t != null) {
            return t.isValid();
        }

        return myOriginal.retrieve() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IncludedXmlElement element = (IncludedXmlElement)o;

        return myParent.equals(element.myParent) && myOriginal.equals(element.myOriginal);
    }

    @Override
    public int hashCode() {
        int result = myOriginal.hashCode();
        result = 31 * result + myParent.hashCode();
        return result;
    }

    public T getOriginal() {
        T element = myRef.get();
        if (element != null) {
            return element;
        }

        element = (T)myOriginal.retrieve();
        if (element == null) {
            throw new PsiInvalidElementAccessException(this);
        }
        myRef = new SoftReference<>(element);
        return element;
    }

    @Nonnull
    @Override
    public T getNavigationElement() {
        return getOriginal();
    }

    @Override
    public PsiFile getContainingFile() {
        return myParent.getContainingFile();
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean processElements(final PsiElementProcessor processor, PsiElement place) {
        final IncludedXmlElement<T> self = this;
        return getOriginal().processElements(
            element -> {
                if (element instanceof XmlTag tag) {
                    XmlTag theirParent = tag.getParentTag();
                    PsiElement parent = getOriginal().equals(theirParent) ? (XmlTag)self : theirParent;
                    return processor.execute(new IncludedXmlTag(tag, parent));
                }
                if (element instanceof XmlAttribute attribute) {
                    XmlTag theirParent = attribute.getParent();
                    XmlTag parent = getOriginal().equals(theirParent) ? (XmlTag)self : theirParent;
                    return processor.execute(new IncludedXmlAttribute(attribute, parent));
                }
                if (element instanceof XmlText text) {
                    XmlTag theirParent = text.getParentTag();
                    XmlTag parent = getOriginal().equals(theirParent) ? (XmlTag)self : theirParent;
                    return processor.execute(new IncludedXmlText(text, parent));
                }
                return processor.execute(element);
            },
            place
        );
    }
}
