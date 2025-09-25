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
package consulo.xml.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.xml.codeInsight.editorActions.XmlAutoPopupHandler;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;

/**
 * @author mike
 * @since 2002-07-23
 */
@ExtensionImpl(id = "xml")
public class XmlCharFilter extends CharFilter {
    @RequiredReadAction
    public static boolean isInXmlContext(Lookup lookup) {
        if (!lookup.isCompletion()) {
            return false;
        }

        PsiElement psiElement = lookup.getPsiElement();
        PsiFile file = lookup.getPsiFile();
        if (!(file instanceof XmlFile) && psiElement != null) {
            file = psiElement.getContainingFile();
        }

        if (file instanceof XmlFile) {
            if (psiElement != null) {
                PsiElement elementToTest = psiElement;
                if (elementToTest instanceof PsiWhiteSpace) {
                    elementToTest = elementToTest.getParent(); // JSPX has whitespace with language Java
                }

                Language language = elementToTest.getLanguage();
                if (!(language instanceof XMLLanguage)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @RequiredReadAction
    public static boolean isWithinTag(Lookup lookup) {
        if (isInXmlContext(lookup)) {
            PsiElement psiElement = lookup.getPsiElement();
            PsiElement parentElement = psiElement != null ? psiElement.getParent() : null;
            if (parentElement instanceof XmlTag) {
                return true;
            }
            if (parentElement instanceof PsiErrorElement && parentElement.getParent() instanceof XmlDocument) {
                return true;
            }

            return (parentElement instanceof XmlDocument || parentElement instanceof XmlText)
                && (psiElement.textMatches("<") || psiElement.textMatches("\""));
        }
        return false;
    }

    @Override
    @RequiredReadAction
    public Result acceptChar(char c, int prefixLength, Lookup lookup) {
        if (!isInXmlContext(lookup)) {
            return null;
        }

        if (Character.isJavaIdentifierPart(c)) {
            return Result.ADD_TO_PREFIX;
        }
        switch (c) {
            case '-':
            case ':':
            case '?':
                return Result.ADD_TO_PREFIX;
            case '/':
                if (isWithinTag(lookup)) {
                    if (prefixLength > 0) {
                        return Result.SELECT_ITEM_AND_FINISH_LOOKUP;
                    }
                    XmlAutoPopupHandler.autoPopupXmlLookup(lookup.getProject(), lookup.getEditor());
                    return Result.HIDE_LOOKUP;
                }
                return Result.ADD_TO_PREFIX;

            case '>':
                if (prefixLength > 0) {
                    return Result.SELECT_ITEM_AND_FINISH_LOOKUP;
                }

            case '\'':
            case '\"':
                return Result.SELECT_ITEM_AND_FINISH_LOOKUP;
            default:
                return null;
        }
    }
}
