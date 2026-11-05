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
package consulo.xml.spellchecker.tokenizer;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import consulo.xml.codeInsight.completion.HtmlCompletionContributor;
import consulo.xml.descriptor.XmlAttributeDescriptor;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlText;
import consulo.xml.language.psi.XmlTokenType;


@ExtensionImpl
public class HtmlSpellcheckingStrategy extends XmlBaseSpellcheckingStrategy {
    @Override
    public Tokenizer getTokenizer(PsiElement element) {
        if (element instanceof PsiComment) {
            return myCommentTokenizer;
        }
        if (element instanceof XmlAttributeValue) {
            return myXmlAttributeTokenizer;
        }
        if (element instanceof XmlText) {
            return myXmlTextTokenizer;
        }
        return SpellcheckingStrategy.EMPTY_TOKENIZER;
    }

    public static boolean shouldParentAttributeBeIgnored(PsiElement element) {
        if (PsiUtilCore.getElementType(element) == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return shouldAttributeBeIgnored(element.getParent());
        }
        return false;
    }

    private static boolean shouldAttributeBeIgnored(PsiElement element) {
        if (element instanceof XmlAttributeValue value && value.getParent() instanceof XmlAttribute attr) {
            return "class".equals(attr.getName()) || shouldBeIgnored(value);
        }
        return false;
    }

    private static boolean shouldBeIgnored(XmlAttributeValue element) {
        PsiElement parent = element.getParent();
        if (parent instanceof XmlAttribute) {
            if (hasSpecificHtmlCompletions(element)) {
                return true;
            }
            XmlAttributeDescriptor descriptor = ((XmlAttribute) parent).getDescriptor();
            if (descriptor != null && (descriptor.isEnumerated() || descriptor.isFixed())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSpecificHtmlCompletions(XmlAttributeValue value) {
        if (!(value.getParent() instanceof XmlAttribute attribute)) {
            return false;
        }
        return HtmlCompletionContributor.hasHtmlAttributesCompletion(value) &&
            HtmlCompletionContributor.addSpecificCompletions(attribute).length != 0;
    }

    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}