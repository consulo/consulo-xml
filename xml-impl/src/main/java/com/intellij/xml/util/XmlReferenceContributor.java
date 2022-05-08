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

import static consulo.xml.patterns.XmlPatterns.xmlAttribute;
import static consulo.xml.patterns.XmlPatterns.xmlAttributeValue;
import static consulo.xml.patterns.XmlPatterns.xmlTag;

import javax.annotation.Nonnull;

import consulo.xml.codeInsight.daemon.impl.analysis.encoding.XmlEncodingReferenceProvider;
import com.intellij.html.impl.providers.MicrodataReferenceProvider;
import com.intellij.html.impl.util.MicrodataUtil;
import consulo.ide.impl.psi.impl.source.resolve.reference.ArbitraryPlaceUrlReferenceProvider;
import consulo.language.impl.psi.path.WebReference;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.psi.filter.AndFilter;
import consulo.language.psi.filter.ScopeFilter;
import consulo.xml.psi.filters.XmlTagFilter;
import consulo.xml.psi.filters.XmlTextFilter;
import consulo.xml.psi.filters.position.NamespaceFilter;
import consulo.language.psi.filter.position.ParentElementFilter;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.DtdReferencesProvider;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.IdReferenceProvider;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.SchemaReferencesProvider;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.URIReferenceProvider;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.XmlBaseReferenceProvider;
import consulo.xml.psi.xml.XmlAttlistDecl;
import consulo.xml.psi.xml.XmlDoctype;
import consulo.xml.psi.xml.XmlElementContentSpec;
import consulo.xml.psi.xml.XmlElementDecl;
import consulo.xml.psi.xml.XmlEntityRef;
import consulo.xml.psi.xml.XmlProcessingInstruction;
import consulo.xml.psi.xml.XmlToken;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.util.ProcessingContext;

/**
 * @author peter
 */
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
		registrar.registerReferenceProvider(xmlAttributeValue(), new ArbitraryPlaceUrlReferenceProvider(), PsiReferenceRegistrar.LOWER_PRIORITY);


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
}
