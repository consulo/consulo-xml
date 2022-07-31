/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.patterns.XmlPatterns;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElementType;
import com.intellij.xml.XmlAttributeDescriptor;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;

import javax.annotation.Nonnull;

import static consulo.xml.codeInsight.completion.XmlCompletionContributor.hasEnumerationReference;
import static consulo.xml.patterns.XmlPatterns.xmlAttribute;
import static consulo.xml.patterns.XmlPatterns.xmlTag;
import static consulo.xml.psi.filters.getters.XmlAttributeValueGetter.getEnumeratedValues;
import static consulo.language.pattern.PlatformPatterns.psiElement;

/**
 * @author yole
 */
@ExtensionImpl(id = "xmlNonFirst", order = "after xml")
public class XmlNonFirstCompletionContributor extends CompletionContributor
{
	public XmlNonFirstCompletionContributor()
	{
		extend(CompletionType.BASIC, psiElement().inside(xmlAttribute()), new XmlAttributeReferenceCompletionProvider());
		extend(CompletionType.BASIC, psiElement().inside(xmlTag()), new TagNameReferenceCompletionProvider());
		extend(CompletionType.BASIC, psiElement().inside(XmlPatterns.xmlAttributeValue()), new CompletionProvider()
		{
			@Override
			public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result)
			{
				PsiElement position = parameters.getPosition();
				if(position.getNode().getElementType() != XmlElementType.XML_ATTRIBUTE_VALUE_TOKEN)
				{
					return;
				}
				XmlAttribute attr = PsiTreeUtil.getParentOfType(position, XmlAttribute.class);
				if(attr != null && !hasEnumerationReference(parameters, result))
				{
					final XmlAttributeDescriptor descriptor = attr.getDescriptor();

					if(descriptor != null)
					{
						if(descriptor.isFixed() && descriptor.getDefaultValue() != null)
						{
							result.addElement(LookupElementBuilder.create(descriptor.getDefaultValue()));
							return;
						}
						for(String value : getEnumeratedValues(attr))
						{
							result.addElement(LookupElementBuilder.create(value));
						}
					}
				}
			}
		});
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return Language.ANY;
	}
}
