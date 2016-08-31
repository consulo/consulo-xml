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

/*
 * Created by IntelliJ IDEA.
 * User: spleaner
 * Date: Jun 19, 2007
 * Time: 3:33:15 PM
 */
package com.intellij.xml.breadcrumbs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import consulo.annotations.RequiredReadAction;

public abstract class BreadcrumbsInfoProvider
{
	public static final ExtensionPointName<BreadcrumbsInfoProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.breadcrumbsInfoProvider");

	@Nullable
	public static BreadcrumbsInfoProvider findProvider(@NotNull final Language language)
	{
		for(final BreadcrumbsInfoProvider provider : BreadcrumbsInfoProvider.EP_NAME.getExtensions())
		{
			Language providerLanguage = provider.getLanguage();
			if(language.isKindOf(providerLanguage))
			{
				return provider;
			}
		}

		return null;
	}

	@NotNull
	public abstract Language getLanguage();

	public boolean validateFileProvider(@NotNull FileViewProvider fileViewProvider)
	{
		return true;
	}

	@RequiredReadAction
	public abstract boolean acceptElement(@NotNull final PsiElement e);

	@Nullable
	@RequiredReadAction
	public PsiElement getParent(@NotNull final PsiElement e)
	{
		return e.getParent();
	}

	@NotNull
	@RequiredReadAction
	public abstract String getElementInfo(@NotNull final PsiElement e);

	@Nullable
	@RequiredReadAction
	public abstract String getElementTooltip(@NotNull final PsiElement e);
}