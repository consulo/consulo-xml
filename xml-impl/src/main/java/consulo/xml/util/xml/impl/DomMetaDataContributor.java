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
package consulo.xml.util.xml.impl;

import consulo.language.psi.PsiElement;
import consulo.language.psi.meta.MetaDataRegistrar;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.DomMetaData;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.meta.MetaDataContributor;

/**
 * @author yole
 */
public class DomMetaDataContributor implements MetaDataContributor
{
	@Override
	public void contributeMetaData(MetaDataRegistrar registrar)
	{
		registrar.registerMetaData(new ElementFilter()
		{
			@Override
			public boolean isAcceptable(Object element, PsiElement context)
			{
				if(element instanceof XmlTag)
				{
					final XmlTag tag = (XmlTag) element;
					final DomElement domElement = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
					if(domElement != null)
					{
						return domElement.getGenericInfo().getNameDomElement(domElement) != null;
					}
				}
				return false;
			}

			@Override
			public boolean isClassAcceptable(Class hintClass)
			{
				return XmlTag.class.isAssignableFrom(hintClass);
			}
		}, DomMetaData.class);
	}
}
