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
package consulo.xml.codeInsight.hint;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.editor.hint.DeclarationRangeHandler;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

@ExtensionImpl
public class TagDeclarationRangeHandler implements DeclarationRangeHandler
{
	@Nonnull
	@Override
	public Class getElementClass()
	{
		return XmlTag.class;
	}

	@Nonnull
	public TextRange getDeclarationRange(@Nonnull final PsiElement container)
	{
		XmlTag xmlTag = (XmlTag) container;
		int endOffset = xmlTag.getTextRange().getStartOffset();

		for(PsiElement child = xmlTag.getFirstChild(); child != null; child = child.getNextSibling())
		{
			endOffset = child.getTextRange().getEndOffset();
			if(child instanceof XmlToken)
			{
				XmlToken token = (XmlToken) child;
				IElementType tokenType = token.getTokenType();
				if(tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END || tokenType == XmlTokenType.XML_TAG_END)
				{
					break;
				}
			}
		}

		return new TextRange(xmlTag.getTextRange().getStartOffset(), endOffset);
	}
}
