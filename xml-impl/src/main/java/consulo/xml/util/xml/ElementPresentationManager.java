/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.xml.util.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.presentation.TypePresentationService;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.component.util.Iconable;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.util.Function;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.reflect.ReflectionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * @author peter
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ElementPresentationManager
{
	private static final Map<Class, Method> ourNameValueMethods = ConcurrentFactoryMap.createMap(key -> {
		for(final Method method : ReflectionUtil.getClassPublicMethods(key))
		{
			if(JavaMethod.getMethod(key, method).getAnnotation(NameValue.class) != null)
			{
				return method;
			}
		}
		return null;
	});

	private final static Function<Object, String> DEFAULT_NAMER = ElementPresentationManager::getElementName;

	public static ElementPresentationManager getInstance()
	{
		return ServiceManager.getService(ElementPresentationManager.class);
	}

	@Nonnull
	public <T> Object[] createVariants(Collection<T> elements)
	{
		return createVariants(elements, (Function<T, String>) DEFAULT_NAMER);
	}

	@Nonnull
	public <T> Object[] createVariants(Collection<T> elements, int iconFlags)
	{
		return createVariants(elements, (Function<T, String>) DEFAULT_NAMER, iconFlags);
	}

	@Nonnull
	public <T> Object[] createVariants(Collection<T> elements, Function<T, String> namer)
	{
		return createVariants(elements, namer, 0);
	}

	/**
	 * Use {@link LookupElementBuilder}
	 */
	@Deprecated
	public abstract Object createVariant(final Object variant, final String name, final PsiElement psiElement);

	@Nonnull
	public abstract <T> Object[] createVariants(Collection<T> elements, Function<T, String> namer, int iconFlags);

	public static <T> NullableFunction<T, String> NAMER()
	{
		return o -> getElementName(o);
	}

	public static final NullableFunction<Object, String> NAMER = o -> getElementName(o);

	public static <T> NullableFunction<T, String> namer()
	{
		//noinspection unchecked
		return (NullableFunction<T, String>) NAMER;
	}

	@Nullable
	public static String getElementName(Object element)
	{
		Object o = invokeNameValueMethod(element);
		if(o == null || o instanceof String)
		{
			return (String) o;
		}
		if(o instanceof GenericValue)
		{
			final GenericValue gv = (GenericValue) o;
			final String s = gv.getStringValue();
			if(s == null)
			{
				final Object value = gv.getValue();
				if(value != null)
				{
					return String.valueOf(value);
				}
			}
			return s;
		}
		return null;
	}

	@Nullable
	public static Object invokeNameValueMethod(final Object element)
	{
		final Method nameValueMethod = findNameValueMethod(element.getClass());
		if(nameValueMethod == null)
		{
			return null;
		}

		return DomReflectionUtil.invokeMethod(nameValueMethod, element);
	}

	public static String getTypeNameForObject(Object o)
	{
		final Object firstImpl = ModelMergerUtil.getFirstImplementation(o);
		o = firstImpl != null ? firstImpl : o;
		String typeName = TypePresentationService.getInstance().getTypeName(o);
		if(typeName != null)
		{
			return typeName;
		}
		if(o instanceof DomElement)
		{
			final DomElement element = (DomElement) o;
			return StringUtil.capitalizeWords(element.getNameStrategy().splitIntoWords(element.getXmlElementName()), true);
		}
		return TypePresentationService.getDefaultTypeName(o.getClass());
	}

	public static Image getIcon(@Nonnull Object o)
	{
		if(o instanceof Iconable)
		{
			return ((Iconable) o).getIcon(0);
		}

		final Image[] icons = getIconsForClass(o.getClass(), o);
		if(icons != null && icons.length > 0)
		{
			return icons[0];
		}
		return null;
	}

	@Nullable
	public static Image getIconForClass(Class clazz)
	{
		return ArrayUtil.getFirstElement(getIconsForClass(clazz, null));
	}

	@Nullable
	private static Image[] getIconsForClass(final Class clazz, @Nullable Object o)
	{
		TypePresentationService service = TypePresentationService.getInstance();
		final Image icon = o == null ? service.getTypeIcon(clazz) : service.getIcon(o);
		if(icon != null)
		{
			return new Image[]{icon};
		}

		return null;
	}

	public static Method findNameValueMethod(final Class<? extends Object> aClass)
	{
		synchronized(ourNameValueMethods)
		{
			return ourNameValueMethods.get(aClass);
		}
	}

	@Nullable
	public static <T> T findByName(Collection<T> collection, final String name)
	{
		return ContainerUtil.find(collection, object -> Comparing.equal(name, getElementName(object), true));
	}
}
