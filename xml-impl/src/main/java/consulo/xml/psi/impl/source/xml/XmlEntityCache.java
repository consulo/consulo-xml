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
package consulo.xml.psi.impl.source.xml;

import consulo.application.util.CachedValue;
import consulo.component.util.ModificationTracker;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.xml.psi.xml.XmlEntityDecl;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;

import java.util.HashMap;
import java.util.Map;

public class XmlEntityCache
{
	static final Object LOCK = new Object();
	private static final Key<Map<String, CachedValue<XmlEntityDecl>>> XML_ENTITY_DECL_MAP = Key.create("XML_ENTITY_DECL_MAP");

	public static void cacheParticularEntity(PsiFile file, XmlEntityDecl decl)
	{
		synchronized(LOCK)
		{
			final Map<String, CachedValue<XmlEntityDecl>> cachingMap = getCachingMap(file);
			final String name = decl.getName();
			if(cachingMap.containsKey(name))
			{
				return;
			}
			final SmartPsiElementPointer declPointer = SmartPointerManager.getInstance(file.getProject()).createSmartPsiElementPointer(decl);

			cachingMap.put(name, CachedValuesManager.getManager(file.getProject()).createCachedValue(() ->
			{
				PsiElement declElement = declPointer.getElement();
				if(declElement instanceof XmlEntityDecl && declElement.isValid() && name.equals(((XmlEntityDecl) declElement).getName()))
				{
					return new CachedValueProvider.Result<>((XmlEntityDecl) declElement, declElement);
				}
				cachingMap.put(name, null);
				return new CachedValueProvider.Result<>(null, ModificationTracker.NEVER_CHANGED);
			}, false));
		}
	}

	static Map<String, CachedValue<XmlEntityDecl>> getCachingMap(final PsiElement targetElement)
	{
		Map<String, CachedValue<XmlEntityDecl>> map = targetElement.getUserData(XML_ENTITY_DECL_MAP);
		if(map == null)
		{
			map = new HashMap<>();
			targetElement.putUserData(XML_ENTITY_DECL_MAP, map);
		}
		return map;
	}

	public static void copyEntityCaches(final PsiFile file, final PsiFile context)
	{
		synchronized(LOCK)
		{
			final Map<String, CachedValue<XmlEntityDecl>> cachingMap = getCachingMap(file);
			for(Map.Entry<String, CachedValue<XmlEntityDecl>> entry : getCachingMap(context).entrySet())
			{
				cachingMap.put(entry.getKey(), entry.getValue());
			}
		}

	}

	public static XmlEntityDecl getCachedEntity(PsiFile file, String name)
	{
		CachedValue<XmlEntityDecl> cachedValue;
		synchronized(LOCK)
		{
			final Map<String, CachedValue<XmlEntityDecl>> cachingMap = getCachingMap(file);
			cachedValue = cachingMap.get(name);
		}
		return cachedValue != null ? cachedValue.getValue() : null;
	}
}
