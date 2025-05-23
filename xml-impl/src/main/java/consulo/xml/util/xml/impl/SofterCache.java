/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.util.SofterReference;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author peter
 */
public class SofterCache<T, V>
{
	private final Function<T, V> myValueProvider;
	private SofterReference<ConcurrentMap<T, Object>> myCache;

	public SofterCache(Function<T, V> valueProvider)
	{
		myValueProvider = valueProvider;
	}

	public static <T, V> SofterCache<T, V> create(Function<T, V> valueProvider)
	{
		return new SofterCache<>(valueProvider);
	}

	public void clearCache()
	{
		myCache = null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public V getCachedValue(T key)
	{
		SofterReference<ConcurrentMap<T, Object>> ref = myCache;
		ConcurrentMap<T, Object> map = ref == null ? null : ref.get();
		if(map == null)
		{
			myCache = new SofterReference<>(map = new ConcurrentHashMap<>());
		}

		Object value = map.get(key);
		if(value == ObjectUtil.NULL)
		{
			return null;
		}
		else if(value != null)
		{
			return (V) value;
		}

		V fetchValue = myValueProvider.apply(key);
		map.put(key, fetchValue == null ? ObjectUtil.NULL : fetchValue);
		return fetchValue;
	}
}
