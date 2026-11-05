/*
 * Copyright 2013-2026 consulo.io
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
package consulo.xml.spellchecker.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.util.HtmlUtils;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.spellchecker.tokenizer.HtmlSpellcheckingStrategy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@ExtensionImpl
public class XmlTextExtractorHtml extends XmlTextExtractor {
    public XmlTextExtractorHtml() {
        super(HTMLLanguage.class);
    }

    @Override
    protected List<TextContent> buildTextContents(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        if (HtmlSpellcheckingStrategy.shouldParentAttributeBeIgnored(element)) {
            return List.of();
        }
        return super.buildTextContents(element, allowedDomains);
    }

    @Override
    protected Function<XmlTag, TagKind> tagClassifier(PsiElement context) {
        if (!Registry.is("grazie.html.concatenate.inline.tag.contents", true)) {
            return super.tagClassifier(context);
        }

        CodeStyleSettings settings = CodeStyle.getSettings(context.getContainingFile());
        Set<String> inlineTags = new HashSet<>(Arrays.asList(settings.HTML_INLINE_ELEMENTS.split(",")));
        inlineTags.add("wbr");
        return tag -> {
            String name = tag.getName();
            if (HtmlUtils.commonBlockElements.contains(name) || isBlockNonTextTag(name)) {
                return TagKind.Block;
            }
            if (inlineTags.contains(name) || isInlineNonTextTag(name)) {
                return TagKind.Inline;
            }
            return TagKind.Unknown;
        };
    }

    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
