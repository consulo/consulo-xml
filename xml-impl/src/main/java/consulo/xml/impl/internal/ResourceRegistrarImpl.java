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
package consulo.xml.impl.internal;

import consulo.xml.javaee.ExternalResourceManagerEx;
import consulo.xml.javaee.ExternalResourceManagerExImpl;
import consulo.xml.javaee.ResourceRegistrar;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class ResourceRegistrarImpl implements ResourceRegistrar
{
	private final Map<String, Map<String, ExternalResource>> myResources = new HashMap<>();
	private final Set<String> myIgnored = new TreeSet<>();

	private ClassLoader myClassLoader = getClass().getClassLoader();

	public void withClassLoader(ClassLoader classLoader, Runnable runnable)
	{
		try
		{
			myClassLoader = classLoader;
			runnable.run();
		}
		finally
		{
			myClassLoader = getClass().getClassLoader();
		}
	}

	public void addStdResourceImpl(String resource, String version, String fileName, @Nullable Class klass)
	{
		Map<String, ExternalResource> map = ExternalResourceManagerExImpl.getMap(myResources, version, true);

		assert map != null;

		ClassLoader classLoader;
		if(klass == null)
		{
			classLoader = myClassLoader;
		}
		else
		{
			classLoader = klass.getClassLoader();
		}
		map.put(resource, new ExternalResource(fileName, Objects.requireNonNull(classLoader)));
	}

	@Override
	public void addStdResource(String resource, @Nullable String version, String fileName)
	{
		addStdResourceImpl(resource, version, fileName, null);
	}

	@Override
	public void addIgnoredResource(String url)
	{
		myIgnored.add(url);
	}

	public void addInternalResource(String resource, String fileName)
	{
		addInternalResource(resource, null, fileName, null);
	}

	public void addInternalResource(String resource, String fileName, Class clazz)
	{
		addInternalResource(resource, null, fileName, clazz);
	}

	public void addInternalResource(String resource, String version, String fileName)
	{
		addInternalResource(resource, version, fileName, null);
	}

	public void addInternalResource(String resource, @Nullable String version, String fileName, @Nullable Class clazz)
	{
		addStdResource(resource, version, ExternalResourceManagerEx.STANDARD_SCHEMAS + fileName);
	}

	@Nonnull
	public Map<String, Map<String, ExternalResource>> getResources()
	{
		return myResources;
	}

	@Nonnull
	public Set<String> getIgnored()
	{
		return myIgnored;
	}
}
