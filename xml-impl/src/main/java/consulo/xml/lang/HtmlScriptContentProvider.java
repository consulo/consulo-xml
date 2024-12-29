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
package consulo.xml.lang;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.lexer.Lexer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface HtmlScriptContentProvider extends LanguageExtension
{
	ExtensionPointCacheKey<HtmlScriptContentProvider, ByLanguageValue<HtmlScriptContentProvider>> KEY = ExtensionPointCacheKey.create("HtmlScriptContentProvider",
			LanguageOneToOne.build());

	@Nullable
	static HtmlScriptContentProvider forLanguage(@Nonnull Language language)
	{
		return Application.get().getExtensionPoint(HtmlScriptContentProvider.class).getOrBuildCache(KEY).get(language);
	}

	/**
	 * @return instance of the <code>com.intellij.psi.tree.IElementType</code> to use in html script tag
	 */
	IElementType getScriptElementType();

	/**
	 * @return highlighting lexer to use in html script tag
	 */
	@Nullable
	Lexer getHighlightingLexer();
}
