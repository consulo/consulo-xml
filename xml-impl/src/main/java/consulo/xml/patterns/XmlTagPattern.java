/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.xml.patterns;

import java.util.Arrays;
import java.util.Collection;

import jakarta.annotation.Nonnull;

import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.InitialPatternCondition;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.meta.PsiMetaData;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import consulo.util.lang.Comparing;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.util.ProcessingContext;

/**
 * @author spleaner
 */
public class XmlTagPattern<Self extends XmlTagPattern<Self>> extends XmlNamedElementPattern<XmlTag, Self>
{
	protected XmlTagPattern()
	{
		super(new InitialPatternCondition<XmlTag>(XmlTag.class)
		{
			@Override
			public boolean accepts(@Nullable final Object o, final ProcessingContext context)
			{
				return o instanceof XmlTag;
			}
		});
	}

	protected XmlTagPattern(@Nonnull final InitialPatternCondition<XmlTag> condition)
	{
		super(condition);
	}

	@Override
	protected String getLocalName(XmlTag tag)
	{
		return tag.getLocalName();
	}

	@Override
	protected String getNamespace(XmlTag tag)
	{
		return tag.getNamespace();
	}

	public Self withAttributeValue(@Nonnull @NonNls final String attributeName, @Nonnull final String attributeValue)
	{
		return with(new PatternCondition<XmlTag>("withAttributeValue")
		{
			@Override
			public boolean accepts(@Nonnull final XmlTag xmlTag, final ProcessingContext context)
			{
				return Comparing.equal(xmlTag.getAttributeValue(attributeName), attributeValue);
			}
		});
	}

	public Self withAnyAttribute(@Nonnull @NonNls final String... attributeNames)
	{
		return with(new PatternCondition<XmlTag>("withAnyAttribute")
		{
			@Override
			public boolean accepts(@Nonnull final XmlTag xmlTag, final ProcessingContext context)
			{
				for(String attributeName : attributeNames)
				{
					if(xmlTag.getAttribute(attributeName) != null)
					{
						return true;
					}
				}
				return false;
			}
		});
	}

	public Self withDescriptor(@Nonnull final ElementPattern<? extends PsiMetaData> metaDataPattern)
	{
		return with(new PatternCondition<XmlTag>("withDescriptor")
		{
			@Override
			public boolean accepts(@Nonnull final XmlTag xmlTag, final ProcessingContext context)
			{
				return metaDataPattern.accepts(xmlTag.getDescriptor());
			}
		});
	}

	public Self isFirstSubtag(@Nonnull final ElementPattern pattern)
	{
		return with(new PatternCondition<XmlTag>("isFirstSubtag")
		{
			@Override
			public boolean accepts(@Nonnull final XmlTag xmlTag, final ProcessingContext context)
			{
				final XmlTag parent = xmlTag.getParentTag();
				return parent != null && pattern.accepts(parent, context) && parent.getSubTags()[0] == xmlTag;
			}
		});
	}

	public Self withFirstSubTag(@Nonnull final ElementPattern<? extends XmlTag> pattern)
	{
		return withSubTags(StandardPatterns.<XmlTag>collection().first(pattern));
	}

	public Self withSubTags(@Nonnull final ElementPattern<? extends Collection<XmlTag>> pattern)
	{
		return with(new PatternCondition<XmlTag>("withSubTags")
		{
			@Override
			public boolean accepts(@Nonnull final XmlTag xmlTag, final ProcessingContext context)
			{
				return pattern.accepts(Arrays.asList(xmlTag.getSubTags()), context);
			}
		});
	}

	public Self withoutAttributeValue(@Nonnull @NonNls final String attributeName, @Nonnull final String attributeValue)
	{
		return and(StandardPatterns.not(withAttributeValue(attributeName, attributeValue)));
	}

	public static class Capture extends XmlTagPattern<Capture>
	{
		static final Capture XML_TAG_PATTERN = new Capture();
	}
}
