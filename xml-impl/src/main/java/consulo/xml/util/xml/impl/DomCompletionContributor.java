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
package consulo.xml.util.xml.impl;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.codeInsight.completion.LegacyCompletionContributor;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.filters.getters.XmlAttributeValueGetter;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;

import javax.annotation.Nonnull;
import java.util.HashSet;

/**
 * @author peter
 */
@ExtensionImpl(id = "dom", order = "first, before xml")
public class DomCompletionContributor extends CompletionContributor
{
	private final GenericValueReferenceProvider myProvider = new GenericValueReferenceProvider();

	@RequiredReadAction
	@Override
	public void fillCompletionVariants(final CompletionParameters parameters, final CompletionResultSet result)
	{
		if(parameters.getCompletionType() != CompletionType.BASIC)
		{
			return;
		}

		if(domKnowsBetter(parameters, result))
		{
			result.stopHere();
		}
	}

	private boolean domKnowsBetter(final CompletionParameters parameters, final CompletionResultSet result)
	{
		final XmlAttributeValue element = PsiTreeUtil.getParentOfType(parameters.getPosition(), XmlAttributeValue.class);
		if(element == null)
		{
			return false;
		}

		if(isSchemaEnumerated(element))
		{
			return false;
		}
		final PsiElement parent = element.getParent();
		if(parent instanceof XmlAttribute)
		{
			XmlAttributeDescriptor descriptor = ((XmlAttribute) parent).getDescriptor();
			if(descriptor != null && descriptor.getDefaultValue() != null)
			{
				final PsiReference[] references = myProvider.getReferencesByElement(element, new ProcessingContext());
				if(references.length > 0)
				{
					return LegacyCompletionContributor.completeReference(parameters, result);
				}
			}
		}
		return false;
	}

	public static boolean isSchemaEnumerated(final PsiElement element)
	{
		if(element instanceof XmlTag)
		{
			final XmlTag simpleContent = XmlUtil.getSchemaSimpleContent((XmlTag) element);
			if(simpleContent != null && XmlUtil.collectEnumerationValues(simpleContent, new HashSet<String>()))
			{
				return true;
			}
		}
		if(element instanceof XmlAttributeValue)
		{
			final PsiElement parent = element.getParent();
			if(parent instanceof XmlAttribute)
			{
				final XmlAttributeDescriptor descriptor = ((XmlAttribute) parent).getDescriptor();
				if(descriptor != null && descriptor.isEnumerated())
				{
					return true;
				}

				String[] enumeratedValues = XmlAttributeValueGetter.getEnumeratedValues((XmlAttribute) parent);
				if(enumeratedValues != null && enumeratedValues.length > 0)
				{
					String value = descriptor == null ? null : descriptor.getDefaultValue();
					if(value == null || enumeratedValues.length != 1 || !value.equals(enumeratedValues[0]))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
