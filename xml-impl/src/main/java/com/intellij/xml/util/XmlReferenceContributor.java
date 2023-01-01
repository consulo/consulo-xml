/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.xml.util;

import com.intellij.html.impl.providers.MicrodataReferenceProvider;
import com.intellij.html.impl.util.MicrodataUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.psi.impl.source.resolve.reference.CommentsReferenceContributor;
import consulo.language.Language;
import consulo.language.impl.psi.path.WebReference;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.*;
import consulo.language.psi.filter.AndFilter;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.psi.filter.ScopeFilter;
import consulo.language.psi.filter.position.ParentElementFilter;
import consulo.language.util.ProcessingContext;
import consulo.xml.codeInsight.daemon.impl.analysis.encoding.XmlEncodingReferenceProvider;
import consulo.xml.psi.filters.XmlTagFilter;
import consulo.xml.psi.filters.XmlTextFilter;
import consulo.xml.psi.filters.position.NamespaceFilter;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.*;
import consulo.xml.psi.xml.*;

import javax.annotation.Nonnull;

import static consulo.xml.patterns.XmlPatterns.*;

/**
 * @author peter
 */
@ExtensionImpl
public class XmlReferenceContributor extends PsiReferenceContributor
{
	@Override
	public void registerReferenceProviders(@Nonnull final PsiReferenceRegistrar registrar)
	{

		final IdReferenceProvider idReferenceProvider = new IdReferenceProvider();

		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, idReferenceProvider.getIdForAttributeNames(), idReferenceProvider.getIdForFilter(), true, idReferenceProvider,
				PsiReferenceRegistrar.DEFAULT_PRIORITY);

		final DtdReferencesProvider dtdReferencesProvider = new DtdReferencesProvider();
		//registerReferenceProvider(null, XmlEntityDecl.class,dtdReferencesProvider);
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlEntityRef.class), dtdReferencesProvider);
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlDoctype.class), dtdReferencesProvider);
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlElementDecl.class), dtdReferencesProvider);
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlAttlistDecl.class), dtdReferencesProvider);
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlElementContentSpec.class), dtdReferencesProvider);
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlToken.class), dtdReferencesProvider);

		PsiReferenceProviderByType commentsReference = PsiReferenceProviderByType.forType(CommentsReferenceContributor.COMMENTS_REFERENCE_PROVIDER_TYPE);

		registrar.registerReferenceProvider(xmlAttributeValue(), commentsReference, PsiReferenceRegistrar.LOWER_PRIORITY);

		URIReferenceProvider uriProvider = new URIReferenceProvider();
		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, null, dtdReferencesProvider.getSystemReferenceFilter(), uriProvider);

		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"href"}, new ScopeFilter(new ParentElementFilter(new AndFilter(new AndFilter(XmlTagFilter.INSTANCE, new
				XmlTextFilter("include")), new NamespaceFilter(XmlUtil.XINCLUDE_URI)), 2)), true, new XmlBaseReferenceProvider(true));

		registrar.registerReferenceProvider(xmlAttributeValue().withLocalName("base").withNamespace(XmlUtil.XML_NAMESPACE_URI), new XmlBaseReferenceProvider(false));

		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{MicrodataUtil.ITEM_TYPE}, null, new MicrodataReferenceProvider());

		final SchemaReferencesProvider schemaReferencesProvider = new SchemaReferencesProvider();

		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, schemaReferencesProvider.getCandidateAttributeNamesForSchemaReferences(), new ScopeFilter(new ParentElementFilter(new
				NamespaceFilter(XmlUtil.SCHEMA_URIS), 2)), schemaReferencesProvider);

		registrar.registerReferenceProvider(xmlAttributeValue(xmlAttribute().withNamespace(XmlUtil.XML_SCHEMA_INSTANCE_URI)).
				withLocalName("type"), schemaReferencesProvider);

		registrar.registerReferenceProvider(xmlAttributeValue(xmlAttribute().withNamespace(XmlUtil.XML_SCHEMA_INSTANCE_URI)).
				withLocalName("noNamespaceSchemaLocation", "schemaLocation"), uriProvider);

		registrar.registerReferenceProvider(xmlAttributeValue().withLocalName("schemaLocation", "namespace").
				withSuperParent(2, xmlTag().withNamespace(XmlUtil.SCHEMA_URIS).withLocalName("import", "include", "redefine")), uriProvider);

		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, null, URIReferenceProvider.ELEMENT_FILTER, true, uriProvider);

		XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"encoding"}, new ScopeFilter(new ParentElementFilter(new ClassFilter(XmlProcessingInstruction.class))), true, new
				XmlEncodingReferenceProvider());

		registrar.registerReferenceProvider(xmlAttributeValue(), new XmlPrefixReferenceProvider());
		registrar.registerReferenceProvider(xmlAttributeValue(), new XmlEnumeratedValueReferenceProvider(), PsiReferenceRegistrar.LOWER_PRIORITY);
		registrar.registerReferenceProvider(xmlTag(), XmlEnumeratedValueReferenceProvider.forTags(), PsiReferenceRegistrar.LOWER_PRIORITY);

		registrar.registerReferenceProvider(xmlAttributeValue().withLocalName("source").withSuperParent(2, xmlTag().withLocalName("documentation").withNamespace(XmlUtil.SCHEMA_URIS)), new
				PsiReferenceProvider()
				{
					@Nonnull
					@Override
					public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context)
					{
						return new PsiReference[]{new WebReference(element)};
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
