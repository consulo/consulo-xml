package consulo.xml.util.xml.impl;

import consulo.application.util.ConcurrentFactoryMap;
import consulo.component.ProcessCanceledException;
import consulo.navigation.Navigatable;
import consulo.util.dataholder.Key;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.util.xml.*;

import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author peter
 */
public class InvocationCache
{
	private static final Map<JavaMethodSignature, Invocation> ourCoreInvocations = new HashMap<JavaMethodSignature, Invocation>();
	private final Map<Method, Invocation> myInvocations = ConcurrentFactoryMap.createMap(key -> ourCoreInvocations.get(new JavaMethodSignature(key)));
	private final Map<Method, JavaMethod> myJavaMethods = ConcurrentFactoryMap.createMap(key -> JavaMethod.getMethod(getType(), key));
	private final Map<JavaMethod, Boolean> myGetters = ConcurrentFactoryMap.createMap(key -> DomImplUtil.isTagValueGetter(key));
	private final Map<JavaMethod, Boolean> mySetters = ConcurrentFactoryMap.createMap(key -> DomImplUtil.isTagValueSetter(key));
	private final Map<JavaMethod, Map<Class, Object>> myMethodAnnotations = ConcurrentFactoryMap.createMap(method -> ConcurrentFactoryMap.createMap(method::getAnnotation));

	private final Map<Class, Object> myClassAnnotations = ConcurrentFactoryMap.createMap(annoClass -> getType().getAnnotation(annoClass));
	private final Class myType;

	static
	{
		addCoreInvocations(DomElement.class);
		addCoreInvocations(Navigatable.class);
		addCoreInvocations(AnnotatedElement.class);
		addCoreInvocations(Object.class);
		ourCoreInvocations.put(new JavaMethodSignature("getUserData", Key.class), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				return handler.getUserData((Key<?>) args[0]);
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("putUserData", Key.class, Object.class), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				//noinspection unchecked
				handler.putUserData((Key) args[0], args[1]);
				return null;
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getXmlElement"), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				return handler.getXmlElement();
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getXmlTag"), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				return handler.getXmlTag();
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getParent"), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				return handler.getParent();
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("accept", DomElementVisitor.class), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				handler.accept((DomElementVisitor) args[0]);
				return null;
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("acceptChildren", DomElementVisitor.class), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				handler.acceptChildren((DomElementVisitor) args[0]);
				return null;
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getAnnotation", Class.class), new Invocation()
		{
			public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
			{
				//noinspection unchecked
				return handler.getAnnotation((Class<Annotation>) args[0]);
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getRawText"), new Invocation()
		{
			public final Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable
			{
				return handler.getValue();
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getXmlAttribute"), new Invocation()
		{
			public final Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable
			{
				return handler.getXmlElement();
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getXmlAttributeValue"), new Invocation()
		{
			@Nullable
			public final Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable
			{
				final XmlAttribute attribute = (XmlAttribute) handler.getXmlElement();
				return attribute != null ? attribute.getValueElement() : null;
			}
		});
		ourCoreInvocations.put(new JavaMethodSignature("getConverter"), new Invocation()
		{
			public final Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable
			{
				try
				{
					return handler.getScalarConverter();
				}
				catch(Throwable e)
				{
					final Throwable cause = e.getCause();
					if(cause instanceof ProcessCanceledException)
					{
						throw (ProcessCanceledException) cause;
					}
					throw new RuntimeException(e);
				}
			}
		});
	}

	private static void addCoreInvocations(final Class<?> aClass)
	{
		for(final Method method : aClass.getDeclaredMethods())
		{
			if("equals".equals(method.getName()))
			{
				ourCoreInvocations.put(new JavaMethodSignature(method), new Invocation()
				{
					public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
					{
						final DomElement proxy = handler.getProxy();
						final Object arg = args[0];
						if(proxy == arg)
						{
							return true;
						}
						if(arg == null)
						{
							return false;
						}

						if(arg instanceof DomElement)
						{
							final DomInvocationHandler handler1 = DomManagerImpl.getDomInvocationHandler(proxy);
							return handler1 != null && handler1.equals(DomManagerImpl.getDomInvocationHandler((DomElement) arg));
						}

						return false;
					}

				});
			}
			else if("hashCode".equals(method.getName()))
			{
				ourCoreInvocations.put(new JavaMethodSignature(method), new Invocation()
				{
					public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
					{
						return handler.hashCode();
					}
				});
			}
			else
			{
				ourCoreInvocations.put(new JavaMethodSignature(method), new Invocation()
				{
					public Object invoke(DomInvocationHandler<?, ?> handler, Object[] args) throws Throwable
					{
						return method.invoke(handler, args);
					}
				});
			}
		}
	}

	public InvocationCache(Class type)
	{
		myType = type;
	}

	public Class getType()
	{
		return myType;
	}

	@Nullable
	public Invocation getInvocation(Method method)
	{
		return myInvocations.get(method);
	}

	public JavaMethod getInternedMethod(Method method)
	{
		return myJavaMethods.get(method);
	}

	public void putInvocation(Method method, Invocation invocation)
	{
		myInvocations.put(method, invocation);
	}

	public boolean isTagValueGetter(JavaMethod method)
	{
		return myGetters.get(method);
	}

	public boolean isTagValueSetter(JavaMethod method)
	{
		return mySetters.get(method);
	}

	@Nullable
	public <T extends Annotation> T getMethodAnnotation(JavaMethod method, Class<T> annoClass)
	{
		return (T) myMethodAnnotations.get(method).get(annoClass);
	}

	@Nullable
	public <T extends Annotation> T getClassAnnotation(Class<T> annoClass)
	{
		return (T) myClassAnnotations.get(annoClass);
	}
}
