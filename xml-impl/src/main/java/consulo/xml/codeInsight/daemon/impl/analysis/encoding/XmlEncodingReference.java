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
package consulo.xml.codeInsight.daemon.impl.analysis.encoding;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.util.io.CharsetToolkit;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlAttributeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
public class XmlEncodingReference implements PsiReference, EmptyResolveMessageProvider, Comparable<XmlEncodingReference> {
    private final XmlAttributeValue myValue;

    private final String myCharsetName;
    private final TextRange myRangeInElement;
    private final int myPriority;

    public XmlEncodingReference(XmlAttributeValue value, String charsetName, TextRange rangeInElement, int priority) {
        myValue = value;
        myCharsetName = charsetName;
        myRangeInElement = rangeInElement;
        myPriority = priority;
    }

    @Override
    @RequiredReadAction
    public PsiElement getElement() {
        return myValue;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public TextRange getRangeInElement() {
        return myRangeInElement;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiElement resolve() {
        return CharsetToolkit.forName(myCharsetName) == null ? null : myValue;
        //if (ApplicationManager.getApplication().isUnitTestMode()) return myValue; // tests do not have full JDK
        //String fqn = charset.getClass().getName();
        //return myValue.getManager().findClass(fqn, GlobalSearchScope.allScope(myValue.getProject()));
    }

    @Nonnull
    @Override
    public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
        return XmlErrorLocalize.unknownEncoding0(referenceText);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getCanonicalText() {
        return myCharsetName;
    }

    @Override
    @RequiredWriteAction
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        return null;
    }

    @Override
    @RequiredWriteAction
    public PsiElement bindToElement(@Nonnull PsiElement element) throws consulo.language.util.IncorrectOperationException {
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        return false;
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public Object[] getVariants() {
        Charset[] charsets = CharsetToolkit.getAvailableCharsets();
        List<LookupElement> suggestions = new ArrayList<>(charsets.length);
        for (Charset charset : charsets) {
            suggestions.add(LookupElementBuilder.create(charset.name()).withCaseSensitivity(false));
        }
        return suggestions.toArray(new LookupElement[suggestions.size()]);
    }

    @Override
    public int compareTo(XmlEncodingReference ref) {
        return myPriority - ref.myPriority;
    }
}
