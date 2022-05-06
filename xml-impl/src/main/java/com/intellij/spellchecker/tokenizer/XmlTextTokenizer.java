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
package com.intellij.spellchecker.tokenizer;

import javax.annotation.Nonnull;

import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.spellchecker.inspections.PlainTextSplitter;

public class XmlTextTokenizer extends Tokenizer<XmlText> {
	@Override
	public void tokenize(@Nonnull XmlText element, TokenConsumer consumer) {
		if (element instanceof PsiLanguageInjectionHost && InjectedLanguageUtil.hasInjections((PsiLanguageInjectionHost)element)) return;
		processChildren(element, consumer);
	}

	private static void processChildren(PsiElement element, TokenConsumer consumer) {
		final PsiElement[] children = element.getChildren();
		for (PsiElement child : children) {
			IElementType elementType = child.getNode().getElementType();
			if (elementType == XmlTokenType.XML_DATA_CHARACTERS) {
				consumer.consumeToken(child, PlainTextSplitter.getInstance());
			}
			else if (elementType == XmlElementType.XML_CDATA) {
				processChildren(child, consumer);
			}
		}
	}
}
