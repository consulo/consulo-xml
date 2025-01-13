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

import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.application.util.CachedValueProvider;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.application.util.RecursionManager;
import consulo.language.psi.PsiModificationTracker;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author peter
 */
class InclusionProvider implements CachedValueProvider<PsiElement[]> {
    //xpointer($1)
    public static Pattern XPOINTER_PATTERN = Pattern.compile("xpointer\\((.*)\\)");

    // /$1(/$2)?/*
    public static Pattern CHILDREN_PATTERN = Pattern.compile("/([^/]*)(/[^/]*)?/\\*");

    private final XmlTag myXincludeTag;

    public InclusionProvider(XmlTag xincludeTag) {
        myXincludeTag = xincludeTag;
    }

    @Nonnull
    public static PsiElement[] getIncludedTags(XmlTag xincludeTag) {
        return CachedValuesManager.getManager(xincludeTag.getProject()).getCachedValue(xincludeTag, new InclusionProvider(xincludeTag));
    }

    public Result<PsiElement[]> compute() {
        PsiElement[] result = RecursionManager.doPreventingRecursion(
            myXincludeTag,
            true,
            () -> computeInclusion(myXincludeTag)
        );
        return Result.create(result == null ? PsiElement.EMPTY_ARRAY : result, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
    }

    private static XmlTag[] extractXpointer(@Nonnull XmlTag rootTag, @Nullable final String xpointer) {
        if (xpointer != null) {
            Matcher matcher = XPOINTER_PATTERN.matcher(xpointer);
            if (matcher.matches()) {
                String pointer = matcher.group(1);
                matcher = CHILDREN_PATTERN.matcher(pointer);
                if (matcher.matches() && matcher.group(1).equals(rootTag.getName())) {
                    return rootTag.getSubTags();
                }
            }
        }

        return new XmlTag[]{rootTag};
    }

    @Nullable
    private static PsiElement[] computeInclusion(final XmlTag xincludeTag) {
        final XmlFile included = XmlIncludeHandler.resolveXIncludeFile(xincludeTag);
        final XmlDocument document = included != null ? included.getDocument() : null;
        final XmlTag rootTag = document != null ? document.getRootTag() : null;
        if (rootTag != null) {
            final String xpointer = xincludeTag.getAttributeValue("xpointer", XmlUtil.XINCLUDE_URI);
            final XmlTag[] includeTag = extractXpointer(rootTag, xpointer);
            PsiElement[] result = new PsiElement[includeTag.length];
            for (int i = 0; i < includeTag.length; i++) {
                result[i] = new IncludedXmlTag(includeTag[i], xincludeTag.getParentTag());
            }
            return result;
        }

        return null;
    }
}
