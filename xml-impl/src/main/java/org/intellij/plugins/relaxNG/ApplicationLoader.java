/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG;

import consulo.xml.javaee.ResourceRegistrar;
import consulo.xml.javaee.StandardResourceProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.AndFilter;
import consulo.language.psi.filter.ClassFilter;
import consulo.xml.psi.filters.position.NamespaceFilter;
import consulo.language.psi.meta.MetaDataRegistrar;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.language.psi.filter.ElementFilter;
import jakarta.inject.Singleton;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncDocument;
import org.intellij.plugins.relaxNG.inspections.RngDomInspection;
import org.intellij.plugins.relaxNG.inspections.UnusedDefineInspection;
import org.intellij.plugins.relaxNG.model.descriptors.RngNsDescriptor;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;
import org.intellij.plugins.relaxNG.xml.dom.impl.RngDefineMetaData;

@Singleton
public class ApplicationLoader
{
	public static final String RNG_NAMESPACE = "http://relaxng.org/ns/structure/1.0";

	public ApplicationLoader()
	{
		registerMetaData();
	}

	private static void registerMetaData()
	{
		final MetaDataRegistrar registrar = MetaDataRegistrar.getInstance();
		registrar.registerMetaData(new AndFilter(new NamespaceFilter(RNG_NAMESPACE), new ClassFilter(XmlDocument.class)), RngNsDescriptor.class);

		registrar.registerMetaData(new ClassFilter(RncDocument.class), RngNsDescriptor.class);

		registrar.registerMetaData(new ElementFilter()
		{
			@Override
			public boolean isAcceptable(Object element, PsiElement context)
			{
				if(element instanceof XmlTag)
				{
					final XmlTag tag = (XmlTag) element;
					final DomElement domElement = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
					return domElement instanceof RngDefine;
				}
				return false;
			}

			@Override
			public boolean isClassAcceptable(Class hintClass)
			{
				return XmlTag.class.isAssignableFrom(hintClass);
			}
		}, RngDefineMetaData.class);
	}

	public static Class[] getInspectionClasses()
	{
		return new Class[]{
				RngDomInspection.class,
				UnusedDefineInspection.class
		};
	}

	public static class ResourceProvider implements StandardResourceProvider
	{
		@Override
		public void registerResources(ResourceRegistrar registrar)
		{
			registrar.addStdResource(RNG_NAMESPACE, "/resources/relaxng.rng", getClass());
			registrar.addIgnoredResource("http://relaxng.org/ns/compatibility/annotations/1.0");
		}
	}
}
