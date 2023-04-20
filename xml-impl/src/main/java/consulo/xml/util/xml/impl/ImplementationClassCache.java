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
package consulo.xml.util.xml.impl;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.util.collection.MultiMap;
import consulo.xml.dom.ImplementationProvider;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * @author peter
 */
class ImplementationClassCache<T extends ImplementationProvider>
{
	private static final Comparator<ImplementationProvider> CLASS_COMPARATOR = (i1, i2) ->
	{
		Class o1 = i1.getImplementationClass();
		Class o2 = i2.getImplementationClass();

		if(o1.isAssignableFrom(o2))
		{
			return 1;
		}
		if(o2.isAssignableFrom(o1))
		{
			return -1;
		}
		if(o1.equals(o2))
		{
			return 0;
		}
		throw new AssertionError("Incompatible implementation classes: " + o1 + " & " + o2);
	};

	private final ExtensionPointCacheKey<T, MultiMap<Class, T>> myExtensionCacheKey;
	private final Class<T> myImplementationClassProvider;
	private final SofterCache<Class, T> myCache = SofterCache.create((Function<Class, T>) dom -> calcImplementationClass(dom));

	ImplementationClassCache(Class<T> implementationClassProvider)
	{
		myImplementationClassProvider = implementationClassProvider;
		myExtensionCacheKey = ExtensionPointCacheKey.create(implementationClassProvider.getName(), walker ->
		{
			MultiMap<Class, T> map = MultiMap.create();
			walker.walk(implProvider -> map.putValue(implProvider.getInterfaceClass(), implProvider));
			return map;
		});
	}

	private MultiMap<Class, T> getMap()
	{
		return Application.get().getExtensionPoint(myImplementationClassProvider).getOrBuildCache(myExtensionCacheKey);
	}

	private T calcImplementationClass(Class concreteInterface)
	{
		final TreeSet<T> set = new TreeSet<>(CLASS_COMPARATOR);
		findImplementationClassDFS(concreteInterface, set);
		if(!set.isEmpty())
		{
			return set.first();
		}
		return null;
	}

	private void findImplementationClassDFS(final Class concreteInterface, SortedSet<T> results)
	{
		final Collection<T> values = getMap().get(concreteInterface);
		for(T value : values)
		{
			if(value.getInterfaceClass() == concreteInterface)
			{
				results.add(value);
				return;
			}
		}
		for(final Class aClass1 : concreteInterface.getInterfaces())
		{
			findImplementationClassDFS(aClass1, results);
		}
	}

	@Nullable
	public T get(Class key)
	{
		T impl = myCache.getCachedValue(key);
		if(impl == null)
		{
			return null;
		}
		return impl.getImplementationClass() == key ? null : impl;
	}
}
