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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.impl.psi.DelegatePsiTarget;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.xml.lang.xml.XMLLanguage;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl(id = "dom")
public class DomDocumentationProvider implements LanguageDocumentationProvider
{
	public String getQuickNavigateInfo(final PsiElement element, PsiElement originalElement)
	{
		return null;
	}

	public List<String> getUrlFor(final PsiElement element, final PsiElement originalElement)
	{
		return null;
	}

	public String generateDoc(PsiElement element, final PsiElement originalElement)
	{
		if(element instanceof PomTargetPsiElement)
		{
			PomTarget target = ((PomTargetPsiElement) element).getTarget();
			if(target instanceof DelegatePsiTarget)
			{
				element = ((DelegatePsiTarget) target).getNavigationElement();
			}
		}
		final DomElement domElement = DomUtil.getDomElement(element);
		if(domElement == null)
		{
			return null;
		}
		ElementPresentationTemplate template = domElement.getChildDescription().getPresentationTemplate();
		if(template != null)
		{
			String documentation = template.createPresentation(domElement).getDocumentation();
			if(documentation != null)
			{
				return documentation;
			}
		}
		return null;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
