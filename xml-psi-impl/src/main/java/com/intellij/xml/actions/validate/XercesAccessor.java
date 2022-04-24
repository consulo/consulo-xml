package com.intellij.xml.actions.validate;

import consulo.util.lang.lazy.LazyValue;
import org.apache.xerces.impl.XMLEntityManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

// FIXME [VISTALL] we can't move it to org.apache.xerces.impl due java9 module package restriction
public class XercesAccessor
{
	private static final Supplier<Method> ourGetDeclaredEntitiesMethod = LazyValue.notNull(() ->
	{
		try
		{
			Method method = XMLEntityManager.class.getDeclaredMethod("getDeclaredEntities");
			method.setAccessible(true);
			return method;
		}
		catch(NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	});

	public static Map<String, XMLEntityManager.Entity> getEntities(XMLEntityManager entityManager)
	{
		try
		{
			return (Map<String, XMLEntityManager.Entity>) ourGetDeclaredEntitiesMethod.get().invoke(entityManager);
		}
		catch(IllegalAccessException | InvocationTargetException e)
		{
			throw new RuntimeException(e);
		}
	}
}
