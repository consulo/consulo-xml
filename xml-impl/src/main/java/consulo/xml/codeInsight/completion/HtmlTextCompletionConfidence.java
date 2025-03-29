/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.CompletionConfidence;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlText;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "htmlText")
public class HtmlTextCompletionConfidence extends CompletionConfidence {
    @Nonnull
    @Override
    @RequiredReadAction
    public ThreeState shouldSkipAutopopup(@Nonnull PsiElement contextElement, @Nonnull PsiFile psiFile, int offset) {
        return shouldSkipAutopopupInHtml(contextElement, offset) ? ThreeState.YES : ThreeState.UNSURE;
    }

    @RequiredReadAction
    public static boolean shouldSkipAutopopupInHtml(@Nonnull PsiElement contextElement, int offset) {
        ASTNode node = contextElement.getNode();
        if (node != null && node.getElementType() == XmlTokenType.XML_DATA_CHARACTERS) {
            PsiElement parent = contextElement.getParent();
            if (parent instanceof XmlText || parent instanceof XmlDocument) {
                String contextElementText = contextElement.getText();
                int endOffset = offset - contextElement.getTextRange().getStartOffset();
                String prefix = contextElementText.substring(0, Math.min(contextElementText.length(), endOffset));
                return !StringUtil.startsWithChar(prefix, '<') && !StringUtil.startsWithChar(prefix, '&');
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
