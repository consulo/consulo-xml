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

import consulo.application.ApplicationManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import consulo.logging.Logger;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.javaee.web.PsiReferenceConverter;
import consulo.xml.psi.xml.*;
import consulo.xml.util.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class GenericValueReferenceProvider extends PsiReferenceProvider
{

	private final static Logger LOG = Logger.getInstance("#GenericValueReferenceProvider");

	@Nonnull
	public final PsiReference[] getReferencesByElement(@Nonnull PsiElement psiElement, @Nonnull final ProcessingContext context)
	{
		final DomManager domManager = DomManager.getDomManager(psiElement.getProject());

		final DomElement domElement;
		if(psiElement instanceof XmlTag)
		{
			domElement = domManager.getDomElement((XmlTag) psiElement);
		}
		else if(psiElement instanceof XmlAttributeValue && psiElement.getParent() instanceof XmlAttribute)
		{
			domElement = domManager.getDomElement((XmlAttribute) psiElement.getParent());
		}
		else
		{
			return PsiReference.EMPTY_ARRAY;
		}

		if(!(domElement instanceof GenericDomValue))
		{
			return PsiReference.EMPTY_ARRAY;
		}

		InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(psiElement.getProject());

		if(psiElement instanceof XmlTag)
		{
			for(XmlText text : ((XmlTag) psiElement).getValue().getTextElements())
			{
				if(injectedLanguageManager.getInjectedPsiFiles(text) != null)
				{
					return PsiReference.EMPTY_ARRAY;
				}
			}
		}
		else
		{
			if(injectedLanguageManager.getInjectedPsiFiles(psiElement) != null)
			{
				return PsiReference.EMPTY_ARRAY;
			}
		}

		final GenericDomValue domValue = (GenericDomValue) domElement;

		final Referencing referencing = domValue.getAnnotation(Referencing.class);
		final Object converter;
		if(referencing == null)
		{
			converter = WrappingConverter.getDeepestConverter(domValue.getConverter(), domValue);
		}
		else
		{
			Class<? extends CustomReferenceConverter> clazz = referencing.value();
			converter = ((ConverterManagerImpl) domManager.getConverterManager()).getInstance(clazz);
		}
		PsiReference[] references = createReferences(domValue, (XmlElement) psiElement, converter);
		if(ApplicationManager.getApplication().isUnitTestMode())
		{
			for(PsiReference reference : references)
			{
				if(!reference.isSoft())
				{
					LOG.error("dom reference should be soft: " + reference + " (created by " + converter + ")");
				}
			}
		}
		return references;
	}

	private static ConvertContext createConvertContext(final PsiElement psiElement, final GenericDomValue domValue)
	{
		return ConvertContextFactory.createConvertContext(domValue);
	}

	@Nullable
	private static DomInvocationHandler getInvocationHandler(final GenericDomValue domValue)
	{
		return DomManagerImpl.getDomInvocationHandler(domValue);
	}

	private PsiReference[] createReferences(final GenericDomValue domValue, final XmlElement psiElement, final Object converter)
	{
		ConvertContext context = createConvertContext(psiElement, domValue);

		List<PsiReference> result = new ArrayList<>();
		String unresolvedText = ElementManipulators.getValueText(psiElement);

		DomReferenceInjector.walk(psiElement.getProject(), DomUtil.getFileElement(domValue).getFileDescription(), each ->
		{
			Collections.addAll(result, each.inject(unresolvedText, psiElement, context));
		});

		Collections.addAll(result, doCreateReferences(domValue, psiElement, converter, context));

		return result.toArray(new PsiReference[result.size()]);
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private PsiReference[] doCreateReferences(GenericDomValue domValue, XmlElement psiElement, Object converter, ConvertContext context)
	{
		if(converter instanceof CustomReferenceConverter)
		{
			final PsiReference[] references =
					((CustomReferenceConverter) converter).createReferences(domValue, psiElement, context);

			if(references.length == 0 && converter instanceof ResolvingConverter)
			{
				return new PsiReference[]{new GenericDomValueReference(domValue)};
			}
			else
			{
				return references;
			}
		}
		if(converter instanceof PsiReferenceConverter)
		{
			return ((PsiReferenceConverter) converter).createReferences(psiElement, true);
		}
		if(converter instanceof ResolvingConverter)
		{
			return new PsiReference[]{new GenericDomValueReference(domValue)};
		}

		final DomInvocationHandler invocationHandler = getInvocationHandler(domValue);
		assert invocationHandler != null;
		final Class clazz = DomUtil.getGenericValueParameter(invocationHandler.getDomElementType());
		if(clazz == null)
		{
			return PsiReference.EMPTY_ARRAY;
		}

		if(ReflectionUtil.isAssignable(Integer.class, clazz))
		{
			return new PsiReference[]{
					new GenericDomValueReference<>((GenericDomValue<Integer>) domValue)
					{
						@Nonnull
						public Object[] getVariants()
						{
							return new Object[]{"0"};
						}
					}
			};
		}
		if(ReflectionUtil.isAssignable(String.class, clazz))
		{
			return PsiReference.EMPTY_ARRAY;
		}

		return PsiReference.EMPTY_ARRAY;
	}
}
