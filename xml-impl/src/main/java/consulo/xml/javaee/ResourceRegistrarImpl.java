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
package consulo.xml.javaee;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class ResourceRegistrarImpl implements ResourceRegistrar
{
	private final Map<String, Map<String, ExternalResourceManagerExImpl.Resource>> myResources = new HashMap<>();
	private final List<String> myIgnored = new ArrayList<>();

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

	@Override
	public void addStdResource(@NonNls String resource, @NonNls String fileName)
	{
		addStdResource(resource, null, fileName, getClass());
	}

	@Override
	public void addStdResource(@NonNls String resource, @NonNls String fileName, Class klass)
	{
		addStdResource(resource, null, fileName, klass);
	}

	public void addStdResourceImpl(@NonNls String resource, @NonNls String version, @NonNls String fileName, @Nullable Class klass)
	{
		Map<String, ExternalResourceManagerExImpl.Resource> map = ExternalResourceManagerExImpl.getMap(myResources, version, true);

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
		map.put(resource, new ExternalResourceManagerExImpl.Resource(fileName, Objects.requireNonNull(classLoader)));
	}

	@Override
	public void addStdResource(@NonNls String resource, @Nullable @NonNls String version, @NonNls String fileName)
	{
		addStdResourceImpl(resource, version, fileName, null);
	}

	@Override
	public void addStdResource(@NonNls String resource, @Nullable @NonNls String version, @NonNls String fileName, Class klass)
	{
		addStdResourceImpl(resource, version, fileName, klass);
	}

	@Override
	public void addIgnoredResource(@NonNls String url)
	{
		myIgnored.add(url);
	}

	public void addInternalResource(@NonNls String resource, @NonNls String fileName)
	{
		addInternalResource(resource, null, fileName, getClass());
	}

	public void addInternalResource(@NonNls String resource, @NonNls String fileName, Class clazz)
	{
		addInternalResource(resource, null, fileName, clazz);
	}

	public void addInternalResource(@NonNls String resource, @NonNls String version, @NonNls String fileName)
	{
		addInternalResource(resource, version, fileName, getClass());
	}

	public void addInternalResource(@NonNls String resource, @Nullable @NonNls String version, @NonNls String fileName, @Nullable Class clazz)
	{
		addStdResource(resource, version, ExternalResourceManagerEx.STANDARD_SCHEMAS + fileName, clazz);
	}

	@Nonnull
	public Map<String, Map<String, ExternalResourceManagerExImpl.Resource>> getResources()
	{
		return myResources;
	}

	@Nonnull
	public List<String> getIgnored()
	{
		return myIgnored;
	}
}
