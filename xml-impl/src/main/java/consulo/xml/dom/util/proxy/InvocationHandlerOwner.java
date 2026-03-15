package consulo.xml.dom.util.proxy;

import consulo.util.lang.reflect.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2020-06-18
 */
public interface InvocationHandlerOwner
{
	static Method METHOD = ReflectionUtil.getMethod(InvocationHandlerOwner.class, "getInvocationHandler");

	static InvocationHandler getHandler(Object o)
	{
		if(o instanceof InvocationHandlerOwner)
		{
			return ((InvocationHandlerOwner) o).getInvocationHandler();
		}

		throw new IllegalArgumentException("Object " + o + " is not InvocationHandlerOwner");
	}

	InvocationHandler getInvocationHandler();
}
