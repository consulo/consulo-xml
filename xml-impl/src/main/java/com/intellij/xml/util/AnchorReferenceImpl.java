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

import com.intellij.xml.XmlBundle;
import com.intellij.xml.XmlExtension;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.*;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Maxim.Mossienko
 */
public class AnchorReferenceImpl implements PsiReference, EmptyResolveMessageProvider, AnchorReference {
    private final String myAnchor;
    private final FileReference myFileReference;
    private final PsiElement myElement;
    private final int myOffset;
    private final boolean mySoft;
    @NonNls
    private static final String ANCHOR_ELEMENT_NAME = "a";
    private static final String MAP_ELEMENT_NAME = "map";
    private static final Key<CachedValue<Map<String, XmlTag>>> ourCachedIdsKey = Key.create("cached.ids");

    AnchorReferenceImpl(
        final String anchor,
        @Nullable final FileReference psiReference,
        final PsiElement element,
        final int offset,
        final boolean soft
    ) {
        myAnchor = anchor;
        myFileReference = psiReference;
        myElement = element;
        myOffset = offset;
        mySoft = soft;
    }

    @RequiredReadAction
    public PsiElement getElement() {
        return myElement;
    }

    @RequiredReadAction
    public TextRange getRangeInElement() {
        return new TextRange(myOffset, myOffset + myAnchor.length());
    }

    @RequiredReadAction
    public PsiElement resolve() {
        if (myAnchor.length() == 0) {
            return myElement;
        }
        Map<String, XmlTag> map = getIdMap();
        final XmlTag tag = map != null ? map.get(myAnchor) : null;
        if (tag != null) {
            XmlAttribute attribute = tag.getAttribute("id");
            if (attribute == null) {
                attribute = tag.getAttribute("name");
            }

            if (attribute == null && MAP_ELEMENT_NAME.equalsIgnoreCase(tag.getName())) {
                attribute = tag.getAttribute("usemap");
            }

            assert attribute != null : tag.getText();
            return attribute.getValueElement();
        }

        return null;
    }

    private static boolean processXmlElements(XmlTag element, PsiElementProcessor<XmlTag> processor) {
        if (!_processXmlElements(element, processor)) {
            return false;
        }

        for (PsiElement next = element.getNextSibling(); next != null; next = next.getNextSibling()) {
            if (next instanceof XmlTag nextTag && !_processXmlElements(nextTag, processor)) {
                return false;
            }
        }

        return true;
    }

    static boolean _processXmlElements(XmlTag element, PsiElementProcessor<XmlTag> processor) {
        if (!processor.execute(element)) {
            return false;
        }
        final XmlTag[] subTags = element.getSubTags();

        for (XmlTag subTag : subTags) {
            if (!_processXmlElements(subTag, processor)) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private Map<String, XmlTag> getIdMap() {
        final XmlFile file = getFile();

        if (file != null) {
            CachedValue<Map<String, XmlTag>> value = file.getUserData(ourCachedIdsKey);
            if (value == null) {
                value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new MapCachedValueProvider(file), false);
                file.putUserData(ourCachedIdsKey, value);
            }

            return value.getValue();
        }
        return null;
    }

    @Nullable
    private static String getAnchorValue(final XmlTag xmlTag) {
        final String attributeValue = xmlTag.getAttributeValue("id");

        if (attributeValue != null) {
            return attributeValue;
        }

        if (ANCHOR_ELEMENT_NAME.equalsIgnoreCase(xmlTag.getName())) {
            final String attributeValue2 = xmlTag.getAttributeValue("name");
            if (attributeValue2 != null) {
                return attributeValue2;
            }
        }

        if (MAP_ELEMENT_NAME.equalsIgnoreCase(xmlTag.getName())) {
            final String map_anchor = xmlTag.getAttributeValue("name");
            if (map_anchor != null) {
                return map_anchor;
            }
        }

        return null;
    }

    @RequiredReadAction
    @Nonnull
    public String getCanonicalText() {
        return myAnchor;
    }

    @RequiredWriteAction
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        return ElementManipulators.getManipulator(myElement).handleContentChange(myElement, getRangeInElement(), newElementName);
    }

    @RequiredWriteAction
    @Nullable
    public PsiElement bindToElement(@Nonnull PsiElement element) throws consulo.language.util.IncorrectOperationException {
        return null;
    }

    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        return element instanceof XmlAttributeValue && myElement.getManager().areElementsEquivalent(element, resolve());
    }

    @RequiredReadAction
    @Nonnull
    public Object[] getVariants() {
        final Map<String, XmlTag> idMap = getIdMap();
        if (idMap == null) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        String[] variants = idMap.keySet().toArray(new String[idMap.size()]);
        LookupElement[] elements = new LookupElement[variants.length];
        for (int i = 0, variantsLength = variants.length; i < variantsLength; i++) {
            elements[i] = LookupElementBuilder.create(variants[i]).withCaseSensitivity(true);
        }
        return elements;
    }

    @Nullable
    private XmlFile getFile() {
        if (myFileReference != null) {
            final PsiElement psiElement = myFileReference.resolve();
            return psiElement instanceof XmlFile ? (XmlFile)psiElement : null;
        }

        final PsiFile containingFile = myElement.getContainingFile();
        if (containingFile instanceof XmlFile) {
            return (XmlFile)containingFile;
        }
        else {
            final XmlExtension extension = XmlExtension.getExtensionByElement(myElement);
            return extension == null ? null : extension.getContainingFile(myElement);
        }
    }

    @RequiredReadAction
    public boolean isSoft() {
        return mySoft;
    }

    @Nonnull
    @Override
    public LocalizeValue buildUnresolvedMessage(@Nonnull String s) {
        final XmlFile xmlFile = getFile();
        return LocalizeValue.localizeTODO(
            xmlFile == null
                ? XmlBundle.message("cannot.resolve.anchor", myAnchor)
                : XmlBundle.message("cannot.resolve.anchor.in.file", myAnchor, xmlFile.getName())
        );
    }

    // separate static class to avoid memory leak via this$0
    private static class MapCachedValueProvider implements CachedValueProvider<Map<String, XmlTag>> {
        private final XmlFile myFile;

        public MapCachedValueProvider(XmlFile file) {
            myFile = file;
        }

        public Result<Map<String, XmlTag>> compute() {
            final Map<String, XmlTag> resultMap = new HashMap<>();
            XmlDocument document = HtmlUtil.getRealXmlDocument(myFile.getDocument());
            final XmlTag rootTag = document != null ? document.getRootTag() : null;

            if (rootTag != null) {
                processXmlElements(
                    rootTag,
                    element -> {
                        final String anchorValue = getAnchorValue(element);

                        if (anchorValue != null) {
                            resultMap.put(anchorValue, element);
                        }
                        return true;
                    }
                );
            }
            return new Result<>(resultMap, myFile);
        }
    }
}
