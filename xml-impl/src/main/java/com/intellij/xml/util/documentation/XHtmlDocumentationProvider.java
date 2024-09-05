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
package com.intellij.xml.util.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;

import javax.annotation.Nonnull;

/**
 * @author maxim
 */
@ExtensionImpl
public class XHtmlDocumentationProvider extends HtmlDocumentationProvider {
    @Nonnull
    @Override
    public Language getLanguage() {
        return XHTMLLanguage.INSTANCE;
    }

    protected String generateDocForHtml(PsiElement element, boolean ommitHtmlSpecifics, XmlTag context, PsiElement originalElement) {
        return super.generateDocForHtml(element, true, context, originalElement);
    }

    protected XmlTag findTagContext(PsiElement context) {
        XmlTag tagBeforeWhiteSpace = findTagBeforeWhiteSpace(context);
        if (tagBeforeWhiteSpace != null) {
            return tagBeforeWhiteSpace;
        }
        return super.findTagContext(context);
    }

    private static XmlTag findTagBeforeWhiteSpace(PsiElement context) {
        if (context instanceof PsiWhiteSpace) {
            PsiElement parent = context.getParent();
            if (parent instanceof XmlText) {
                if (parent.getPrevSibling() instanceof XmlTag prevTag) {
                    return prevTag;
                }
            }
            else if (parent instanceof XmlTag parentTag) {
                return parentTag;
            }
        }

        return null;
    }

    protected boolean isAttributeContext(PsiElement context) {
        return findTagBeforeWhiteSpace(context) == null && super.isAttributeContext(context);
    }
}
