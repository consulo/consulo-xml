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
package org.intellij.plugins.relaxNG;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.psi.filter.position.PatternFilter;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.patterns.XmlNamedElementPattern;
import org.intellij.plugins.relaxNG.references.PrefixReferenceProvider;

import javax.annotation.Nonnull;

import static consulo.xml.patterns.XmlPatterns.*;

/**
 * @author peter
 */
@ExtensionImpl
public class RelaxNGReferenceContributor extends PsiReferenceContributor
{
	private static final XmlNamedElementPattern RNG_TAG_PATTERN = xmlTag().withNamespace(ApplicationLoader.RNG_NAMESPACE);

	private static final XmlNamedElementPattern.XmlAttributePattern NAME_ATTR_PATTERN = xmlAttribute("name");

	private static final XmlNamedElementPattern.XmlAttributePattern NAME_PATTERN = NAME_ATTR_PATTERN.withParent(
			RNG_TAG_PATTERN.withLocalName("element", "attribute"));

	@Override
	public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar)
	{
		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{
				"name"
		}, new PatternFilter(xmlAttributeValue().withParent(NAME_PATTERN)), true, new PrefixReferenceProvider());

		//    final XmlAttributeValuePattern id = xmlAttributeValue().withParent(xmlAttribute()).with(IdRefProvider.HAS_ID_REF_TYPE);
		//    final XmlAttributeValuePattern idref = xmlAttributeValue().withParent(xmlAttribute()).with(IdRefProvider.HAS_ID_TYPE);
		//    registry.registerXmlAttributeValueReferenceProvider(null, new PatternFilter(or(id, idref)), false, new IdRefProvider());

	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
