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
package com.intellij.util.xml;

import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import com.intellij.psi.xml.*;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author peter
 */
public class EvaluatedXmlNameImpl implements EvaluatedXmlName
{
	private static final Key<CachedValue<Map<String, List<String>>>> NAMESPACE_PROVIDER_KEY = Key.create("NamespaceProvider");
	private static final Map<EvaluatedXmlNameImpl, EvaluatedXmlNameImpl> ourInterned = new ConcurrentHashMap<EvaluatedXmlNameImpl, EvaluatedXmlNameImpl>();

	private final XmlName myXmlName;
	private final String myNamespaceKey;
	private final boolean myEqualToParent;

	private EvaluatedXmlNameImpl(@Nonnull final XmlName xmlName, @Nullable final String namespaceKey, final boolean equalToParent)
	{
		myXmlName = xmlName;
		myNamespaceKey = namespaceKey;
		myEqualToParent = equalToParent;
	}

	@Nonnull
	public final String getLocalName()
	{
		return myXmlName.getLocalName();
	}

	public final XmlName getXmlName()
	{
		return myXmlName;
	}

	public final EvaluatedXmlName evaluateChildName(@Nonnull final XmlName name)
	{
		String namespaceKey = name.getNamespaceKey();
		final boolean equalToParent = Comparing.equal(namespaceKey, myNamespaceKey);
		if(namespaceKey == null)
		{
			namespaceKey = myNamespaceKey;
		}
		return createEvaluatedXmlName(name, namespaceKey, equalToParent);
	}

	public String toString()
	{
		return (myNamespaceKey == null ? "" : myNamespaceKey + " : ") + myXmlName.getLocalName();
	}

	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || !(o instanceof EvaluatedXmlNameImpl))
		{
			return false;
		}

		final EvaluatedXmlNameImpl that = (EvaluatedXmlNameImpl) o;

		if(myEqualToParent != that.myEqualToParent)
		{
			return false;
		}
		if(myNamespaceKey != null ? !myNamespaceKey.equals(that.myNamespaceKey) : that.myNamespaceKey != null)
		{
			return false;
		}
		if(!myXmlName.equals(that.myXmlName))
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = myXmlName.hashCode();
		result = 31 * result + (myNamespaceKey != null ? myNamespaceKey.hashCode() : 0);
		result = 31 * result + (myEqualToParent ? 1 : 0);
		return result;
	}

	public final boolean isNamespaceAllowed(DomFileElement element, String namespace)
	{
		if(myNamespaceKey == null || myEqualToParent)
		{
			return true;
		}
		final XmlFile file = element.getFile();
		return isNamespaceAllowed(namespace, getAllowedNamespaces(file));
	}

	@Nonnull
	private List<String> getAllowedNamespaces(final XmlFile file)
	{
		CachedValue<Map<String, List<String>>> value = file.getUserData(NAMESPACE_PROVIDER_KEY);
		if(value == null)
		{
			file.putUserData(NAMESPACE_PROVIDER_KEY, value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<Map<String, List<String>>>()
			{
				public Result<Map<String, List<String>>> compute()
				{
					final Map<String, List<String>> map = ConcurrentFactoryMap.createMap(key -> {
						final DomFileDescription<?> description = DomManager.getDomManager(file.getProject()).getDomFileDescription(file);
						if(description == null)
						{
							return Collections.emptyList();
						}
						return description.getAllowedNamespaces(key, file);
					});
					return Result.create(map, file);
				}
			}, false));
		}

		final List<String> list = value.getValue().get(myNamespaceKey);
		assert list != null;
		return list;
	}

	private static boolean isNamespaceAllowed(final String namespace, final List<String> list)
	{
		return list.contains(namespace) || StringUtil.isEmpty(namespace) && list.isEmpty();

	}

	public final boolean isNamespaceAllowed(String namespace, final XmlFile file, boolean qualified)
	{
		return myNamespaceKey == null || myEqualToParent && !qualified || isNamespaceAllowed(namespace, getNamespaceList(file));
	}

	@Nonnull
	@NonNls
	public final String getNamespace(@Nonnull XmlElement parentElement, final XmlFile file)
	{
		final String xmlElementNamespace = getXmlElementNamespace(parentElement);
		if(myNamespaceKey != null && !myEqualToParent)
		{
			final List<String> strings = getAllowedNamespaces(file);
			if(!strings.isEmpty() && !strings.contains(xmlElementNamespace))
			{
				return strings.get(0);
			}
		}
		return xmlElementNamespace;
	}

	private static String getXmlElementNamespace(final XmlElement parentElement)
	{
		if(parentElement instanceof XmlTag)
		{
			return ((XmlTag) parentElement).getNamespace();
		}
		if(parentElement instanceof XmlAttribute)
		{
			return ((XmlAttribute) parentElement).getNamespace();
		}
		if(parentElement instanceof XmlFile)
		{
			final XmlDocument document = ((XmlFile) parentElement).getDocument();
			if(document != null)
			{
				final XmlTag tag = document.getRootTag();
				if(tag != null)
				{
					return tag.getNamespace();
				}
			}
			return "";
		}
		throw new AssertionError("Can't get namespace of " + parentElement);
	}

	private List<String> getNamespaceList(final XmlFile file)
	{
		return getAllowedNamespaces(file);
	}

	public static EvaluatedXmlNameImpl createEvaluatedXmlName(@Nonnull final XmlName xmlName, @Nullable final String namespaceKey, boolean equalToParent)
	{
		final EvaluatedXmlNameImpl name = new EvaluatedXmlNameImpl(xmlName, namespaceKey, equalToParent);
		final EvaluatedXmlNameImpl interned = ourInterned.get(name);
		if(interned != null)
		{
			return interned;
		}
		ourInterned.put(name, name);
		return name;
	}
}
