/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.xml.util.documentation.HtmlDescriptorsTable;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.lexer.DelegateLexer;
import consulo.language.lexer.Lexer;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.xml.lang.HtmlScriptContentProvider;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author Maxim.Mossienko
 */
public abstract class BaseHtmlLexer extends DelegateLexer
{
	protected static final int BASE_STATE_MASK = 0x3F;
	private static final int SEEN_STYLE = 0x40;
	private static final int SEEN_TAG = 0x80;
	private static final int SEEN_SCRIPT = 0x100;
	private static final int SEEN_ATTRIBUTE = 0x200;
	private static final int SEEN_CONTENT_TYPE = 0x400;
	private static final int SEEN_STYLESHEET_TYPE = 0x800;
	protected static final int BASE_STATE_SHIFT = 11;

	protected boolean seenTag;
	protected boolean seenAttribute;
	protected boolean seenStyle;
	protected boolean seenScript;

	@Nullable
	protected String scriptType = null;
	@Nullable
	protected String styleType = null;

	private final boolean caseInsensitive;
	protected boolean seenContentType;
	protected boolean seenStylesheetType;
	private CharSequence cachedBufferSequence;
	private Lexer lexerOfCacheBufferSequence;

	static final TokenSet TOKENS_TO_MERGE = TokenSet.create(XmlTokenType.XML_COMMENT_CHARACTERS, XmlTokenType.XML_WHITE_SPACE, XmlTokenType.XML_REAL_WHITE_SPACE, XmlTokenType
			.XML_ATTRIBUTE_VALUE_TOKEN, XmlTokenType.XML_DATA_CHARACTERS, XmlTokenType.XML_TAG_CHARACTERS);

	public interface TokenHandler
	{
		void handleElement(Lexer lexer);
	}

	public class XmlNameHandler implements TokenHandler
	{
		@NonNls
		private static final String TOKEN_SCRIPT = "script";
		@NonNls
		private static final String TOKEN_STYLE = "style";
		@NonNls
		private static final String TOKEN_ON = "on";

		@Override
		public void handleElement(Lexer lexer)
		{
			final CharSequence buffer;
			if(lexerOfCacheBufferSequence == lexer)
			{
				buffer = cachedBufferSequence;
			}
			else
			{
				cachedBufferSequence = lexer.getBufferSequence();
				buffer = cachedBufferSequence;
				lexerOfCacheBufferSequence = lexer;
			}
			final char firstCh = buffer.charAt(lexer.getTokenStart());

			if(seenScript && !seenTag)
			{
				seenContentType = false;
				if(((firstCh == 'l' || firstCh == 't') || (caseInsensitive && (firstCh == 'L' || firstCh == 'T'))))
				{
					@NonNls String name = TreeUtil.getTokenText(lexer);
					seenContentType = Comparing.strEqual("language", name, !caseInsensitive) || Comparing.strEqual("type", name, !caseInsensitive);
					return;
				}
			}
			if(seenStyle && !seenTag)
			{
				seenStylesheetType = false;
				if(firstCh == 't' || caseInsensitive && firstCh == 'T')
				{
					seenStylesheetType = Comparing.strEqual(TreeUtil.getTokenText(lexer), "type", !caseInsensitive);
					return;
				}
			}

			if(firstCh != 'o' && firstCh != 's' && (!caseInsensitive || (firstCh != 'S' && firstCh != 'O')))
			{
				return; // optimization
			}

			String name = TreeUtil.getTokenText(lexer);
			if(caseInsensitive)
			{
				name = name.toLowerCase();
			}

			final boolean style = name.equals(TOKEN_STYLE);
			final int state = getState() & BASE_STATE_MASK;
			final boolean script = name.equals(TOKEN_SCRIPT) || ((name.startsWith(TOKEN_ON) && name.indexOf(':') == -1 && !isHtmlTagState(state) && HtmlDescriptorsTable.getAttributeDescriptor(name)
					!= null));

			if(style || script)
			{
				// encountered tag name in end of tag
				if(seenTag)
				{
					if(isHtmlTagState(state))
					{
						seenTag = false;
					}
					return;
				}

				seenStyle = style;
				seenScript = script;

				if(!isHtmlTagState(state))
				{
					seenAttribute = true;
				}
			}
		}
	}

	class XmlAttributeValueEndHandler implements TokenHandler
	{
		@Override
		public void handleElement(Lexer lexer)
		{
			if(seenAttribute)
			{
				seenStyle = false;
				seenScript = false;
				seenAttribute = false;
			}
			seenContentType = false;
			seenStylesheetType = false;
		}
	}

	class XmlAttributeValueHandler implements TokenHandler
	{
		@Override
		public void handleElement(Lexer lexer)
		{
			if(seenContentType && seenScript && !seenAttribute)
			{
				@NonNls String mimeType = TreeUtil.getTokenText(lexer);
				scriptType = caseInsensitive ? mimeType.toLowerCase(Locale.US) : mimeType;
			}
			if(seenStylesheetType && seenStyle && !seenAttribute)
			{
				@NonNls String type = TreeUtil.getTokenText(lexer).trim();
				styleType = caseInsensitive ? type.toLowerCase(Locale.US) : type;
			}
		}
	}

	@Nullable
	protected Language getScriptLanguage()
	{
		String mimeType = scriptType != null ? scriptType.trim() : null;
		if(mimeType == null)
		{
			return null;
		}
		Collection<Language> instancesByMimeType = Language.findInstancesByMimeType(mimeType);
		return instancesByMimeType.isEmpty() ? null : instancesByMimeType.iterator().next();
	}

	@Nullable
	protected LanguageVersion getStyleLanguageVersion()
	{
		Language cssLanguage = ExternalPluginHelper.getCssLanguage();
		if(cssLanguage != null && styleType != null && !"text/css".equals(styleType))
		{
			for(LanguageVersion languageVersion : cssLanguage.getVersions())
			{
				for(String mimeType : languageVersion.getMimeTypes())
				{
					if(styleType.equals(mimeType))
					{
						return languageVersion;
					}
				}
			}
		}
		//noinspection RequiredXAction
		return cssLanguage == null ? null : LanguageVersionUtil.findDefaultVersion(cssLanguage);
	}

	@Nullable
	protected IElementType getCurrentScriptElementType()
	{
		HtmlScriptContentProvider scriptContentProvider = findScriptContentProvider(scriptType);
		return scriptContentProvider == null ? null : scriptContentProvider.getScriptElementType();
	}

	@Nullable
	protected IElementType getCurrentStylesheetElementType()
	{
		LanguageVersion languageVersion = getStyleLanguageVersion();
		if(languageVersion != null)
		{
			for(EmbeddedTokenTypesProvider provider : EmbeddedTokenTypesProvider.EXTENSION_POINT_NAME.getExtensions())
			{
				if(provider.isMyVersion(languageVersion))
				{
					return provider.getElementType();
				}
			}
		}
		return null;
	}

	@Nullable
	protected HtmlScriptContentProvider findScriptContentProvider(@Nullable String mimeType)
	{
		if(StringUtil.isEmpty(mimeType))
		{
			Language javaScriptLanguage = ExternalPluginHelper.getJavaScriptLanguage();
			return javaScriptLanguage != null ? HtmlScriptContentProvider.forLanguage(javaScriptLanguage) : null;
		}
		Collection<Language> instancesByMimeType = Language.findInstancesByMimeType(mimeType.trim());
		if(instancesByMimeType.isEmpty() && mimeType.contains("template"))
		{
			instancesByMimeType = Collections.singletonList(HTMLLanguage.INSTANCE);
		}
		for(Language language : instancesByMimeType)
		{
			HtmlScriptContentProvider scriptContentProvider = HtmlScriptContentProvider.forLanguage(language);
			if(scriptContentProvider != null)
			{
				return scriptContentProvider;
			}
		}
		return null;
	}

	class XmlTagClosedHandler implements TokenHandler
	{
		@Override
		public void handleElement(Lexer lexer)
		{
			if(seenAttribute)
			{
				seenScript = false;
				seenStyle = false;

				seenAttribute = false;
			}
			else
			{
				if(seenStyle || seenScript)
				{
					seenTag = true;
				}
			}
		}
	}

	class XmlTagEndHandler implements TokenHandler
	{
		@Override
		public void handleElement(Lexer lexer)
		{
			seenStyle = false;
			seenScript = false;
			seenAttribute = false;
			seenContentType = false;
			seenStylesheetType = false;
			scriptType = null;
			styleType = null;
		}
	}

	private final HashMap<IElementType, TokenHandler> tokenHandlers = new HashMap<>();

	protected BaseHtmlLexer(Lexer _baseLexer, boolean _caseInsensitive)
	{
		super(_baseLexer);
		caseInsensitive = _caseInsensitive;

		XmlNameHandler value = new XmlNameHandler();
		tokenHandlers.put(XmlTokenType.XML_NAME, value);
		tokenHandlers.put(XmlTokenType.XML_TAG_NAME, value);
		tokenHandlers.put(XmlTokenType.XML_TAG_END, new XmlTagClosedHandler());
		tokenHandlers.put(XmlTokenType.XML_END_TAG_START, new XmlTagEndHandler());
		tokenHandlers.put(XmlTokenType.XML_EMPTY_ELEMENT_END, new XmlTagEndHandler());
		tokenHandlers.put(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, new XmlAttributeValueEndHandler());
		tokenHandlers.put(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, new XmlAttributeValueHandler());
	}

	protected void registerHandler(IElementType elementType, TokenHandler value)
	{
		final TokenHandler tokenHandler = tokenHandlers.get(elementType);

		if(tokenHandler != null)
		{
			final TokenHandler newHandler = value;
			value = new TokenHandler()
			{
				@Override
				public void handleElement(final Lexer lexer)
				{
					tokenHandler.handleElement(lexer);
					newHandler.handleElement(lexer);
				}
			};
		}

		tokenHandlers.put(elementType, value);
	}

	@Override
	public void start(@Nonnull final CharSequence buffer, final int startOffset, final int endOffset, final int initialState)
	{
		initState(initialState);
		super.start(buffer, startOffset, endOffset, initialState & BASE_STATE_MASK);
	}

	private void initState(final int initialState)
	{
		seenScript = (initialState & SEEN_SCRIPT) != 0;
		seenStyle = (initialState & SEEN_STYLE) != 0;
		seenTag = (initialState & SEEN_TAG) != 0;
		seenAttribute = (initialState & SEEN_ATTRIBUTE) != 0;
		seenContentType = (initialState & SEEN_CONTENT_TYPE) != 0;
		seenStylesheetType = (initialState & SEEN_STYLESHEET_TYPE) != 0;
		lexerOfCacheBufferSequence = null;
		cachedBufferSequence = null;
	}

	protected int skipToTheEndOfTheEmbeddment()
	{
		Lexer base = getDelegate();
		int tokenEnd = base.getTokenEnd();
		int lastState = 0;
		int lastStart = 0;

		final CharSequence buf = base.getBufferSequence();
		final char[] bufArray = CharArrayUtil.fromSequenceWithoutCopying(buf);

		if(seenTag)
		{
			FoundEnd:
			while(true)
			{
				FoundEndOfTag:
				while(base.getTokenType() != XmlTokenType.XML_END_TAG_START)
				{
					if(base.getTokenType() == XmlTokenType.XML_COMMENT_CHARACTERS)
					{
						// we should terminate on first occurence of </
						final int end = base.getTokenEnd();

						for(int i = base.getTokenStart(); i < end; ++i)
						{
							if((bufArray != null ? bufArray[i] : buf.charAt(i)) == '<' && i + 1 < end && (bufArray != null ? bufArray[i + 1] : buf.charAt(i + 1)) == '/')
							{
								tokenEnd = i;
								lastStart = i - 1;
								lastState = 0;

								break FoundEndOfTag;
							}
						}
					}

					lastState = base.getState();
					tokenEnd = base.getTokenEnd();
					lastStart = base.getTokenStart();
					if(tokenEnd == getBufferEnd())
					{
						break FoundEnd;
					}
					base.advance();
				}

				// check if next is script
				if(base.getTokenType() != XmlTokenType.XML_END_TAG_START)
				{ // we are inside comment
					base.start(buf, lastStart + 1, getBufferEnd(), lastState);
					base.getTokenType();
					base.advance();
				}
				else
				{
					base.advance();
				}

				while(XmlTokenType.WHITESPACES.contains(base.getTokenType()))
				{
					base.advance();
				}

				if(base.getTokenType() == XmlTokenType.XML_NAME)
				{
					String name = TreeUtil.getTokenText(base);
					if(caseInsensitive)
					{
						name = name.toLowerCase();
					}

					if(endOfTheEmbeddment(name))
					{
						break; // really found end
					}
				}
			}

			base.start(buf, lastStart, getBufferEnd(), lastState);
			base.getTokenType();
		}
		else if(seenAttribute)
		{
			while(true)
			{
				if(!isValidAttributeValueTokenType(base.getTokenType()))
				{
					break;
				}

				tokenEnd = base.getTokenEnd();
				lastState = base.getState();
				lastStart = base.getTokenStart();

				if(tokenEnd == getBufferEnd())
				{
					break;
				}
				base.advance();
			}

			base.start(buf, lastStart, getBufferEnd(), lastState);
			base.getTokenType();
		}
		return tokenEnd;
	}

	protected boolean endOfTheEmbeddment(String name)
	{
		return (hasSeenScript() && XmlNameHandler.TOKEN_SCRIPT.equals(name)) || (hasSeenStyle() && XmlNameHandler.TOKEN_STYLE.equals(name)) || CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
				.equalsIgnoreCase(name);
	}

	protected boolean isValidAttributeValueTokenType(final IElementType tokenType)
	{
		return tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || tokenType == XmlTokenType.XML_ENTITY_REF_TOKEN || tokenType == XmlTokenType.XML_CHAR_ENTITY_REF;
	}

	@Override
	public void advance()
	{
		super.advance();
		IElementType type = getDelegate().getTokenType();
		TokenHandler tokenHandler = tokenHandlers.get(type);
		if(tokenHandler != null)
		{
			tokenHandler.handleElement(this);
		}
	}


	@Override
	public int getState()
	{
		int state = super.getState();

		state |= ((seenScript) ? SEEN_SCRIPT : 0);
		state |= ((seenTag) ? SEEN_TAG : 0);
		state |= ((seenStyle) ? SEEN_STYLE : 0);
		state |= ((seenAttribute) ? SEEN_ATTRIBUTE : 0);
		state |= ((seenContentType) ? SEEN_CONTENT_TYPE : 0);
		state |= ((seenStylesheetType) ? SEEN_STYLESHEET_TYPE : 0);

		return state;
	}

	protected final boolean hasSeenStyle()
	{
		return seenStyle;
	}

	protected final boolean hasSeenAttribute()
	{
		return seenAttribute;
	}

	protected final boolean hasSeenTag()
	{
		return seenTag;
	}

	protected boolean hasSeenScript()
	{
		return seenScript;
	}

	protected abstract boolean isHtmlTagState(int state);
}
