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
package consulo.xml.ide.highlighter;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import consulo.xml.lexer.DtdLexer;
import consulo.xml.lexer.XHtmlHighlightingLexer;
import consulo.xml.lexer.XmlHighlightingLexer;
import consulo.xml.editor.XmlHighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.xml.psi.xml.XmlTokenType;
import consulo.codeEditor.HighlighterColors;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.lexer.Lexer;

public class XmlFileHighlighter extends SyntaxHighlighterBase
{
	private static final Map<IElementType, TextAttributesKey> keys1;
	private static final Map<IElementType, TextAttributesKey> keys2;

	static
	{
		keys1 = new HashMap<>();
		keys2 = new HashMap<>();

		keys1.put(XmlTokenType.XML_DATA_CHARACTERS, XmlHighlighterColors.XML_TAG_DATA);

		keys1.put(XmlTokenType.XML_COMMENT_START, XmlHighlighterColors.XML_COMMENT);
		keys1.put(XmlTokenType.XML_COMMENT_END, XmlHighlighterColors.XML_COMMENT);
		keys1.put(XmlTokenType.XML_COMMENT_CHARACTERS, XmlHighlighterColors.XML_COMMENT);
		keys1.put(XmlTokenType.XML_CONDITIONAL_COMMENT_END, XmlHighlighterColors.XML_COMMENT);
		keys1.put(XmlTokenType.XML_CONDITIONAL_COMMENT_END_START, XmlHighlighterColors.XML_COMMENT);
		keys1.put(XmlTokenType.XML_CONDITIONAL_COMMENT_START, XmlHighlighterColors.XML_COMMENT);
		keys1.put(XmlTokenType.XML_CONDITIONAL_COMMENT_START_END, XmlHighlighterColors.XML_COMMENT);

		keys1.put(XmlTokenType.XML_START_TAG_START, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_END_TAG_START, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_TAG_END, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_EMPTY_ELEMENT_END, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_TAG_NAME, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.TAG_WHITE_SPACE, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_NAME, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_CONDITIONAL_IGNORE, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_CONDITIONAL_INCLUDE, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_EQ, XmlHighlighterColors.XML_TAG);
		keys1.put(XmlTokenType.XML_TAG_CHARACTERS, XmlHighlighterColors.XML_TAG);

		keys2.put(XmlTokenType.XML_TAG_NAME, XmlHighlighterColors.XML_TAG_NAME);
		keys2.put(XmlTokenType.XML_CONDITIONAL_INCLUDE, XmlHighlighterColors.XML_TAG_NAME);
		keys2.put(XmlTokenType.XML_CONDITIONAL_INCLUDE, XmlHighlighterColors.XML_TAG_NAME);
		keys2.put(XmlTokenType.XML_NAME, XmlHighlighterColors.XML_ATTRIBUTE_NAME);
		keys2.put(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);
		keys2.put(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);
		keys2.put(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);
		keys2.put(XmlTokenType.XML_EQ, XmlHighlighterColors.XML_ATTRIBUTE_NAME);
		keys2.put(XmlTokenType.XML_TAG_CHARACTERS, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);

		keys1.put(XmlTokenType.XML_BAD_CHARACTER, HighlighterColors.BAD_CHARACTER);

		keys1.put(XmlTokenType.XML_DECL_START, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_CONDITIONAL_SECTION_START, XmlHighlighterColors.XML_PROLOGUE);
		keys2.put(XmlTokenType.XML_CONDITIONAL_SECTION_START, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_CONDITIONAL_SECTION_END, XmlHighlighterColors.XML_PROLOGUE);
		keys2.put(XmlTokenType.XML_CONDITIONAL_SECTION_END, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_DECL_END, XmlHighlighterColors.XML_PROLOGUE);
		keys2.put(XmlTokenType.XML_DECL_END, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_PI_START, XmlHighlighterColors.XML_PROLOGUE);
		keys1.put(XmlTokenType.XML_PI_END, XmlHighlighterColors.XML_PROLOGUE);
		keys1.put(XmlTokenType.XML_DOCTYPE_END, XmlHighlighterColors.XML_PROLOGUE);
		keys2.put(XmlTokenType.XML_DOCTYPE_END, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_DOCTYPE_START, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_DOCTYPE_START, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_DOCTYPE_SYSTEM, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_DOCTYPE_SYSTEM, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_ATTLIST_DECL_START, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_ATTLIST_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_ELEMENT_DECL_START, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_ELEMENT_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

		keys1.put(XmlTokenType.XML_ENTITY_DECL_START, XmlHighlighterColors.XML_TAG);
		keys2.put(XmlTokenType.XML_ENTITY_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

		keys2.put(XmlTokenType.XML_CHAR_ENTITY_REF, XmlHighlighterColors.XML_ENTITY_REFERENCE);
		keys2.put(XmlTokenType.XML_ENTITY_REF_TOKEN, XmlHighlighterColors.XML_ENTITY_REFERENCE);
	}

	private final boolean myIsDtd;
	private boolean myIsXHtml;

	public XmlFileHighlighter()
	{
		this(false);
	}

	public XmlFileHighlighter(boolean dtd)
	{
		myIsDtd = dtd;
	}

	public XmlFileHighlighter(boolean dtd, boolean xhtml)
	{
		myIsDtd = dtd;
		myIsXHtml = xhtml;
	}

	@Override
	@Nonnull
	public Lexer getHighlightingLexer()
	{
		if(myIsDtd)
		{
			return new DtdLexer(true);
		}
		else if(myIsXHtml)
		{
			return new XHtmlHighlightingLexer();
		}
		else
		{
			return new XmlHighlightingLexer();
		}
	}

	@Override
	@Nonnull
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
	{
		return pack(keys1.get(tokenType), keys2.get(tokenType));
	}

	public static void registerEmbeddedTokenAttributes(Map<IElementType, TextAttributesKey> _keys1, Map<IElementType, TextAttributesKey> _keys2)
	{
		if(_keys1 != null)
		{
			keys1.putAll(_keys1);
		}

		if(_keys2 != null)
		{
			keys2.putAll(_keys2);
		}
	}
}
