package com.intellij.xml.actions.validate;

import consulo.application.util.NotNullLazyValue;
import org.apache.xerces.impl.XMLEntityManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

// FIXME [VISTALL] we can't move it to org.apache.xerces.impl due java9 module package restriction
public class XercesAccessor
{
	private static final NotNullLazyValue<Method> ourGetDeclaredEntitiesMethod = NotNullLazyValue.createValue(() ->
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
			return (Map<String, XMLEntityManager.Entity>) ourGetDeclaredEntitiesMethod.getValue().invoke(entityManager);
		}
		catch(IllegalAccessException | InvocationTargetException e)
		{
			throw new RuntimeException(e);
		}
	}
}
