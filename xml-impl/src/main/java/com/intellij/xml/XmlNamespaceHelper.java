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
package com.intellij.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class XmlNamespaceHelper {
    private static final ExtensionPointName<XmlNamespaceHelper> EP_NAME =
        ExtensionPointName.create(XmlNamespaceHelper.class);

    public static XmlNamespaceHelper getHelper(PsiFile file) {
        for (XmlNamespaceHelper extension : EP_NAME.getExtensionList()) {
            if (extension.isAvailable(file)) {
                return extension;
            }
        }
        throw new UnsupportedOperationException("DefaultXmlNamespaceHelper not registered");
    }

    protected abstract boolean isAvailable(PsiFile file);

    public interface Runner<P, T extends Throwable> {
        void run(P param) throws T;
    }

    @Nullable
    public String getNamespacePrefix(PsiElement element) {
        final PsiElement tag = element instanceof XmlTag ? element : element.getParent();
        if (tag instanceof XmlTag) {
            return ((XmlTag)tag).getNamespacePrefix();
        }
        else {
            return null;
        }
    }

    public abstract void insertNamespaceDeclaration(
        @Nonnull final XmlFile file,
        @Nullable final Editor editor,
        @NonNls @Nonnull final Set<String> possibleNamespaces,
        @NonNls @Nullable final String nsPrefix,
        @Nullable Runner<String, IncorrectOperationException> runAfter
    ) throws IncorrectOperationException;

    public boolean qualifyWithPrefix(final String namespacePrefix, final PsiElement element, final Document document)
        throws IncorrectOperationException {
        final PsiElement tag = element instanceof XmlTag ? element : element.getParent();
        if (tag instanceof XmlTag xmlTag) {
            final String prefix = xmlTag.getNamespacePrefix();
            if (!prefix.equals(namespacePrefix)) {
                final String name = namespacePrefix + ":" + xmlTag.getLocalName();
                xmlTag.setName(name);
            }
            return true;
        }
        return false;
    }

    @Nonnull
    public abstract Set<String> guessUnboundNamespaces(@Nonnull PsiElement element, final XmlFile file);

    @Nonnull
    public abstract Set<String> getNamespacesByTagName(@Nonnull final String tagName, @Nonnull final XmlFile context);

    public String getNamespaceAlias(@Nonnull final XmlFile file) {
        return XmlLocalize.namespaceAlias().get();
    }
}
