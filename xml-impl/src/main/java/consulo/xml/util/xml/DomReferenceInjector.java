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
package consulo.xml.util.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;

@ExtensionAPI(ComponentScope.PROJECT)
public interface DomReferenceInjector
{
	static void walk(@Nonnull Project project, @Nonnull DomFileDescription<?> fileDescription, @Nonnull Consumer<DomReferenceInjector> consumer)
	{
		project.getExtensionPoint(DomReferenceInjector.class).forEachExtensionSafe(domReferenceInjector ->
		{
			if(domReferenceInjector.isAvaliable(fileDescription))
			{
				consumer.accept(domReferenceInjector);
			}
		});
	}

	boolean isAvaliable(DomFileDescription<?> fileDescription);

	@Nullable
	String resolveString(@Nullable String unresolvedText, @Nonnull ConvertContext context);

	@Nonnull
	PsiReference[] inject(@Nullable String unresolvedText, @Nonnull PsiElement element, @Nonnull ConvertContext context);
}
