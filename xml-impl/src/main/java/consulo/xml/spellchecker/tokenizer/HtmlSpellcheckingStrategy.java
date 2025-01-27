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
import consulo.language.Language;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlText;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class HtmlSpellcheckingStrategy extends XmlBaseSpellcheckingStrategy
{
	@Nonnull
	@Override
	public Tokenizer getTokenizer(PsiElement element)
	{
		if(element instanceof PsiComment)
		{
			return myCommentTokenizer;
		}
		if(element instanceof XmlAttributeValue)
		{
			return myXmlAttributeTokenizer;
		}
		if(element instanceof XmlText)
		{
			return myXmlTextTokenizer;
		}
		return SpellcheckingStrategy.EMPTY_TOKENIZER;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}