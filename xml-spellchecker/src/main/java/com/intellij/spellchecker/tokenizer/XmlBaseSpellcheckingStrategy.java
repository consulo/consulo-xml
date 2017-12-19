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
package com.intellij.spellchecker.tokenizer;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlText;
import com.intellij.spellchecker.inspections.TextSplitter;

public class XmlBaseSpellcheckingStrategy extends SpellcheckingStrategy
{
	protected final Tokenizer<XmlAttributeValue> myXmlAttributeTokenizer = new XmlAttributeValueTokenizer();
	protected final Tokenizer<XmlText> myXmlTextTokenizer = new XmlTextTokenizer();

	@NotNull
	public Tokenizer getTokenizer(PsiElement element)
	{
		if(element instanceof XmlAttributeValue)
		{
			return myXmlAttributeTokenizer;
		}
		if(element instanceof XmlText)
		{
			return myXmlTextTokenizer;
		}

		return EMPTY_TOKENIZER;
	}

	public static class XmlAttributeValueTokenizer extends Tokenizer<XmlAttributeValue>
	{
		public void tokenize(@NotNull final XmlAttributeValue element, final TokenConsumer consumer)
		{
			if(element instanceof PsiLanguageInjectionHost && InjectedLanguageUtil.hasInjections((PsiLanguageInjectionHost) element))
			{
				return;
			}

			final String valueTextTrimmed = element.getValue().trim();
			// do not inspect colors like #00aaFF
			if(valueTextTrimmed.startsWith("#") && valueTextTrimmed.length() <= 7 && isHexString(valueTextTrimmed.substring(1)))
			{
				return;
			}

			consumer.consumeToken(element, TextSplitter.getInstance());
		}

		private static boolean isHexString(final String s)
		{
			for(int i = 0; i < s.length(); i++)
			{
				if(!StringUtil.isHexDigit(s.charAt(i)))
				{
					return false;
				}
			}
			return true;
		}
	}
}
