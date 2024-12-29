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
package consulo.xml.psi.impl.source.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.impl.source.xml.behavior.CDATAOnAnyEncodedPolicy;

import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface XmlPsiPolicy extends LanguageExtension
{
	ExtensionPointCacheKey<XmlPsiPolicy, ByLanguageValue<XmlPsiPolicy>> KEY = ExtensionPointCacheKey.create("XmlPsiPolicy", LanguageOneToOne.build(new CDATAOnAnyEncodedPolicy()));

	@Nonnull
	static XmlPsiPolicy forLanguage(@Nonnull Language language)
	{
		return Application.get().getExtensionPoint(XmlPsiPolicy.class).getOrBuildCache(KEY).requiredGet(language);
	}

	ASTNode encodeXmlTextContents(String displayText, PsiElement text);
}
