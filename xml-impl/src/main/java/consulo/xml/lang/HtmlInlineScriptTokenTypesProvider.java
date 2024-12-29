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
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface HtmlInlineScriptTokenTypesProvider extends LanguageExtension
{
	ExtensionPointCacheKey<HtmlInlineScriptTokenTypesProvider, ByLanguageValue<HtmlInlineScriptTokenTypesProvider>> KEY = ExtensionPointCacheKey.create("HtmlInlineScriptTokenTypesProvider", LanguageOneToOne.build());

	@Nullable
	static HtmlInlineScriptTokenTypesProvider forLanguage(@Nonnull Language language)
	{
		return Application.get().getExtensionPoint(HtmlInlineScriptTokenTypesProvider.class).getOrBuildCache(KEY).get(language);
	}

	IElementType getElementType();

	FileType getFileType();
}
