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
package consulo.xml.codeInsight.highlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.HighlightErrorFilter;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
@ExtensionImpl
public class HtmlClosingTagErrorFilter extends HighlightErrorFilter {
    @Override
    @RequiredReadAction
    public boolean shouldHighlightErrorElement(@Nonnull PsiErrorElement element) {
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null || (psiFile.getViewProvider().getBaseLanguage() != HTMLLanguage.INSTANCE
            && HTMLLanguage.INSTANCE != element.getLanguage())) {
            return true;
        }

        PsiElement[] children = element.getChildren();
        return children.length <= 0
            || !(children[0] instanceof XmlToken token)
            || XmlTokenType.XML_END_TAG_START != token.getTokenType()
            || !XmlErrorLocalize.xmlParsingClosingTagMatchesNothing().equals(element.getErrorDescriptionValue());
    }
}
