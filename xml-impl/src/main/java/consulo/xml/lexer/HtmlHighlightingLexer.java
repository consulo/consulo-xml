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
package consulo.xml.lexer;

import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.lexer.FlexAdapter;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.MergingLexerAdapter;
import consulo.language.plain.PlainTextLanguage;
import consulo.logging.Logger;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.lang.HtmlInlineScriptTokenTypesProvider;
import consulo.xml.lang.HtmlScriptContentProvider;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nullable;

public class HtmlHighlightingLexer extends BaseHtmlLexer
{
	private static final Logger LOG = Logger.getInstance(HtmlHighlightingLexer.class);

	private static final int EMBEDDED_LEXER_ON = 0x1 << BASE_STATE_SHIFT;
	private static final int EMBEDDED_LEXER_STATE_SHIFT = BASE_STATE_SHIFT + 1;

	private Lexer embeddedLexer;
	private Lexer styleLexer;
	private Lexer scriptLexer;
	private boolean hasNoEmbeddments;

	public class XmlEmbeddmentHandler implements TokenHandler
	{
		public void handleElement(Lexer lexer)
		{
			if(!hasSeenStyle() && !hasSeenScript() || hasNoEmbeddments)
			{
				return;
			}
			final IElementType tokenType = lexer.getTokenType();

			if((tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN && hasSeenAttribute()) ||
					(tokenType == XmlTokenType.XML_DATA_CHARACTERS && hasSeenTag()) ||
					tokenType == XmlTokenType.XML_COMMENT_CHARACTERS && hasSeenTag()
			)
			{
				setEmbeddedLexer();

				if(embeddedLexer != null)
				{
					embeddedLexer.start(
							getBufferSequence(),
							HtmlHighlightingLexer.super.getTokenStart(),
							skipToTheEndOfTheEmbeddment(),
							embeddedLexer instanceof EmbedmentLexer ? ((EmbedmentLexer) embeddedLexer).getEmbeddedInitialState(tokenType) : 0
					);

					if(embeddedLexer.getTokenType() == null)
					{
						// no content for embeddment
						embeddedLexer = null;
					}
				}
			}
		}
	}

	public class ElEmbeddmentHandler implements TokenHandler
	{
		public void handleElement(Lexer lexer)
		{
			setEmbeddedLexer();
			if(embeddedLexer != null)
			{
				embeddedLexer.start(getBufferSequence(), HtmlHighlightingLexer.super.getTokenStart(), HtmlHighlightingLexer.super.getTokenEnd());
			}
		}
	}

	public HtmlHighlightingLexer()
	{
		this(new MergingLexerAdapter(new FlexAdapter(new _HtmlLexer()), TOKENS_TO_MERGE), true);
	}

	protected HtmlHighlightingLexer(Lexer lexer, boolean caseInsensitive)
	{
		super(lexer, caseInsensitive);

		XmlEmbeddmentHandler value = new XmlEmbeddmentHandler();
		registerHandler(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, value);
		registerHandler(XmlTokenType.XML_DATA_CHARACTERS, value);
		registerHandler(XmlTokenType.XML_COMMENT_CHARACTERS, value);
	}

	public void start(CharSequence buffer, int startOffset, int endOffset, int initialState)
	{
		super.start(buffer, startOffset, endOffset, initialState);

		if((initialState & EMBEDDED_LEXER_ON) != 0)
		{
			int state = initialState >> EMBEDDED_LEXER_STATE_SHIFT;
			setEmbeddedLexer();
			LOG.assertTrue(embeddedLexer != null);
			embeddedLexer.start(buffer, startOffset, skipToTheEndOfTheEmbeddment(), state);
		}
		else
		{
			embeddedLexer = null;
			scriptLexer = null;
		}
	}

	private void setEmbeddedLexer()
	{
		Lexer newLexer = null;
		if(hasSeenStyle())
		{
			if(styleLexer == null)
			{
				Language cssLanguage = ExternalPluginHelper.getCssLanguage();
				if(cssLanguage != null)
				{
					styleLexer = SyntaxHighlighterFactory.getSyntaxHighlighter(cssLanguage, null, null).getHighlightingLexer();
				}
			}

			newLexer = styleLexer;
		}
		else if(hasSeenScript())
		{
			if(scriptLexer == null)
			{
				if(hasSeenTag())
				{
					HtmlScriptContentProvider provider = findScriptContentProvider(scriptType);
					if(provider != null)
					{
						scriptLexer = provider.getHighlightingLexer();
					}
					else
					{
						scriptLexer = SyntaxHighlighterFactory.getSyntaxHighlighter(PlainTextLanguage.INSTANCE, null, null).getHighlightingLexer();
					}
				}
				else if(hasSeenAttribute())
				{
					FileType inlineScriptFileType = ExternalPluginElementTypeHolder.getInstance().getInlineScriptFileType();
					SyntaxHighlighter syntaxHighlighter =
							(inlineScriptFileType != null) ? SyntaxHighlighterFactory.getSyntaxHighlighter(inlineScriptFileType, null, null) : null;
					scriptLexer = syntaxHighlighter != null ? syntaxHighlighter.getHighlightingLexer() : null;
				}
			}
			newLexer = scriptLexer;
		}
		else
		{
			newLexer = createELLexer(newLexer);
		}

		if(newLexer != null)
		{
			embeddedLexer = newLexer;
		}
	}

	@Nullable
	protected Lexer createELLexer(Lexer newLexer)
	{
		return newLexer;
	}

	public void advance()
	{
		if(embeddedLexer != null)
		{
			embeddedLexer.advance();
			if(embeddedLexer.getTokenType() == null)
			{
				embeddedLexer = null;
			}
		}

		if(embeddedLexer == null)
		{
			super.advance();
		}
	}

	public IElementType getTokenType()
	{
		if(embeddedLexer != null)
		{
			return embeddedLexer.getTokenType();
		}
		else
		{
			IElementType tokenType = super.getTokenType();

			// TODO: fix no DOCTYPE highlighting
			if(tokenType == null)
			{
				return tokenType;
			}

			if(tokenType == XmlTokenType.XML_NAME)
			{
				// we need to convert single xml_name for tag name and attribute name into to separate
				// lex types for the highlighting!
				final int state = getState() & BASE_STATE_MASK;

				if(isHtmlTagState(state))
				{
					tokenType = XmlTokenType.XML_TAG_NAME;
				}
			}
			else if(tokenType == XmlTokenType.XML_WHITE_SPACE || tokenType == XmlTokenType.XML_REAL_WHITE_SPACE)
			{
				if(hasSeenTag() && (hasSeenStyle() || hasSeenScript()))
				{
					tokenType = XmlTokenType.XML_WHITE_SPACE;
				}
				else
				{
					tokenType = (getState() != 0) ? XmlTokenType.TAG_WHITE_SPACE : XmlTokenType.XML_REAL_WHITE_SPACE;
				}
			}
			else if(tokenType == XmlTokenType.XML_CHAR_ENTITY_REF ||
					tokenType == XmlTokenType.XML_ENTITY_REF_TOKEN
			)
			{
				// we need to convert char entity ref & entity ref in comments as comment chars
				final int state = getState() & BASE_STATE_MASK;
				if(state == _HtmlLexer.COMMENT)
				{
					return XmlTokenType.XML_COMMENT_CHARACTERS;
				}
			}
			return tokenType;
		}
	}

	public int getTokenStart()
	{
		if(embeddedLexer != null)
		{
			return embeddedLexer.getTokenStart();
		}
		else
		{
			return super.getTokenStart();
		}
	}

	public int getTokenEnd()
	{
		if(embeddedLexer != null)
		{
			return embeddedLexer.getTokenEnd();
		}
		else
		{
			return super.getTokenEnd();
		}
	}

	public int getState()
	{
		int state = super.getState();

		state |= ((embeddedLexer != null) ? EMBEDDED_LEXER_ON : 0);
		if(embeddedLexer != null)
		{
			state |= (embeddedLexer.getState() << EMBEDDED_LEXER_STATE_SHIFT);
		}

		return state;
	}

	protected boolean isHtmlTagState(int state)
	{
		return state == _HtmlLexer.START_TAG_NAME || state == _HtmlLexer.END_TAG_NAME ||
				state == _HtmlLexer.START_TAG_NAME2 || state == _HtmlLexer.END_TAG_NAME2;
	}

	public void setHasNoEmbeddments(boolean hasNoEmbeddments)
	{
		this.hasNoEmbeddments = hasNoEmbeddments;
	}
}
