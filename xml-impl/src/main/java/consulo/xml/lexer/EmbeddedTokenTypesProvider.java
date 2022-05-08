/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.component.extension.ExtensionPointName;
import consulo.language.ast.IElementType;
import consulo.language.version.LanguageVersion;

import javax.annotation.Nonnull;

public interface EmbeddedTokenTypesProvider
{
	ExtensionPointName<EmbeddedTokenTypesProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.intellij.xml.embeddedTokenTypesProvider");

	/**
	 * @return name of provider, doesn't related to language name.
	 */
	@Nonnull
	String getName();

	@Nonnull
	IElementType getElementType();

	default boolean isMyVersion(@Nonnull LanguageVersion languageVersion)
	{
		return getElementType().getLanguage() == languageVersion.getLanguage();
	}
}
