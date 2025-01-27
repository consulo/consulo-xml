/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.progress.ProgressManager;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.sem.SemElement;
import consulo.language.sem.SemKey;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.proxy.advanced.AdvancedProxyBuilder;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartFMap;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.dom.util.proxy.InvocationHandlerOwner;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.AnnotatedElement;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.events.DomEvent;
import consulo.xml.util.xml.reflect.*;
import consulo.xml.util.xml.stubs.AttributeStub;
import consulo.xml.util.xml.stubs.DomStub;
import consulo.xml.util.xml.stubs.ElementStub;
import consulo.xml.util.xml.stubs.StubParentStrategy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author peter
 */
public abstract class DomInvocationHandler<T extends AbstractDomChildDescriptionImpl, Stub extends DomStub> extends UserDataHolderBase implements InvocationHandler, DomElement, SemElement
{
	private static final Logger LOG = Logger.getInstance(DomInvocationHandler.class);
	public static final Method ACCEPT_METHOD = ReflectionUtil.getMethod(DomElement.class, "accept", DomElementVisitor.class);
	public static final Method ACCEPT_CHILDREN_METHOD = ReflectionUtil.getMethod(DomElement.class, "acceptChildren", DomElementVisitor.class);
	private static final JavaMethod ourGetValue = JavaMethod.getMethod(GenericValue.class, new JavaMethodSignature("getValue"));

	private final Type myType;
	private final DomManagerImpl myManager;
	private final EvaluatedXmlName myTagName;
	private final T myChildDescription;
	private DomParentStrategy myParentStrategy;
	private volatile long myLastModCount;

	@Nullable
	private DomElement myProxy;
	private DomGenericInfoEx myGenericInfo;
	private final InvocationCache myInvocationCache;
	private volatile Converter myScalarConverter = null;
	private volatile SmartFMap<Method, Invocation> myAccessorInvocations = SmartFMap.emptyMap();
	@Nullable
	protected final Stub myStub;

	protected DomInvocationHandler(Type type, DomParentStrategy parentStrategy,
								   @Nonnull final EvaluatedXmlName tagName,
								   final T childDescription,
								   final DomManagerImpl manager,
								   boolean dynamic,
								   @Nullable Stub stub)
	{
		myManager = manager;
		myParentStrategy = parentStrategy;
		myTagName = tagName;
		myChildDescription = childDescription;
		myStub = stub;
		myLastModCount = manager.getPsiModificationCount();

		myType = narrowType(type);

		myInvocationCache = manager.getApplicationComponent().getInvocationCache(getRawType());
		refreshGenericInfo(dynamic);
		if(stub != null)
		{
			stub.setHandler(this);
		}
	}

	protected Type narrowType(@Nonnull Type nominalType)
	{
		return nominalType;
	}

	@Nullable
	public DomElement getParent()
	{
		final DomInvocationHandler handler = getParentHandler();
		return handler == null ? null : handler.getProxy();
	}

	protected final void assertValid()
	{
		final String s = checkValidity();
		if(s != null)
		{
			throw new AssertionError(myType.toString() + " @" + hashCode() + "\nclass=" + getClass() + "\nxml=" + getXmlElement() + "; " + s);
		}
	}

	@Nullable
	final DomInvocationHandler getParentHandler()
	{
		return getParentStrategy().getParentHandler();
	}

	@Nullable
	public Stub getStub()
	{
		return myStub;
	}

	@Nonnull
	public final Type getDomElementType()
	{
		return myType;
	}

	@Nullable
	protected String getValue()
	{
		final XmlTag tag = getXmlTag();
		return tag == null ? null : getTagValue(tag);
	}

	protected void setValue(@Nullable final String value)
	{
		final XmlTag tag = ensureTagExists();
		myManager.runChange(new Runnable()
		{
			public void run()
			{
				setTagValue(tag, value);
			}
		});
		myManager.fireEvent(new DomEvent(getProxy(), false));
	}

	public void copyFrom(final DomElement other)
	{
		if(other == getProxy())
		{
			return;
		}
		assert other.getDomElementType().equals(myType) : "Can't copy from " + other.getDomElementType() + " to " + myType;

		if(!DomUtil.hasXml(other))
		{
			undefine();
			return;
		}

		myManager.performAtomicChange(new Runnable()
		{
			public void run()
			{
				ensureXmlElementExists();
				final DomGenericInfoEx genericInfo = getGenericInfo();
				for(final AttributeChildDescriptionImpl description : genericInfo.getAttributeChildrenDescriptions())
				{
					description.getDomAttributeValue(DomInvocationHandler.this).setStringValue(description.getDomAttributeValue(other).getStringValue());
				}
				for(final DomFixedChildDescription description : genericInfo.getFixedChildrenDescriptions())
				{
					final List<? extends DomElement> list = description.getValues(getProxy());
					final List<? extends DomElement> otherValues = description.getValues(other);
					for(int i = 0; i < list.size(); i++)
					{
						final DomElement otherValue = otherValues.get(i);
						final DomElement value = list.get(i);
						if(!DomUtil.hasXml(otherValue))
						{
							value.undefine();
						}
						else
						{
							value.copyFrom(otherValue);
						}
					}
				}
				for(final DomCollectionChildDescription description : genericInfo.getCollectionChildrenDescriptions())
				{
					for(final DomElement value : description.getValues(getProxy()))
					{
						value.undefine();
					}
					for(final DomElement otherValue : description.getValues(other))
					{
						description.addValue(getProxy(), otherValue.getDomElementType()).copyFrom(otherValue);
					}
				}

				final String stringValue = DomManagerImpl.getDomInvocationHandler(other).getValue();
				if(StringUtil.isNotEmpty(stringValue))
				{
					setValue(stringValue);
				}
			}
		});

		if(!myManager.getSemService().isInsideAtomicChange())
		{
			myManager.fireEvent(new DomEvent(getProxy(), false));
		}
	}

	public <T extends DomElement> T createStableCopy()
	{
		XmlTag tag = getXmlTag();
		if(tag != null && tag.isPhysical())
		{
			final DomElement existing = myManager.getDomElement(tag);
			assert existing != null : existing + "\n---------\n" + tag.getParent().getText() + "\n-----------\n" + tag.getText();
			assert getProxy().equals(existing) : existing + "\n---------\n" + tag.getParent().getText() + "\n-----------\n" + tag.getText() + "\n----\n" + this + " != " +
					DomManagerImpl.getDomInvocationHandler(existing);
			final SmartPsiElementPointer<XmlTag> pointer =
					SmartPointerManager.getInstance(myManager.getProject()).createSmartPsiElementPointer(tag);
			return myManager.createStableValue(new StableCopyFactory<T>(pointer, myType, getClass()));
		}
		return (T) createPathStableCopy();
	}

	protected DomElement createPathStableCopy()
	{
		throw new UnsupportedOperationException();
	}

	public final <T extends DomElement> T createMockCopy(final boolean physical)
	{
		final T copy = myManager.createMockElement((Class<? extends T>) getRawType(), getProxy().getModule(), physical);
		copy.copyFrom(getProxy());
		return copy;
	}

	@Nonnull
	public String getXmlElementNamespace()
	{
		final DomInvocationHandler parent = getParentHandler();
		assert parent != null : "this operation should be performed on the DOM having a physical parent, your DOM may be not very fresh";
		final XmlElement element = parent.getXmlElement();
		assert element != null;
		return getXmlName().getNamespace(element, getFile());
	}

	@Nullable
	public String getXmlElementNamespaceKey()
	{
		return getXmlName().getXmlName().getNamespaceKey();
	}

	public final Module getModule()
	{
		final Module module = ModuleUtilCore.findModuleForPsiElement(getFile());
		return module != null ? module : DomUtil.getFile(this).getUserData(DomManagerImpl.MOCK_ELEMENT_MODULE);
	}

	public XmlTag ensureTagExists()
	{
		assertValid();

		XmlTag tag = getXmlTag();
		if(tag != null)
		{
			return tag;
		}

		tag = setEmptyXmlTag();
		setXmlElement(tag);

		final DomElement element = getProxy();
		myManager.fireEvent(new DomEvent(element, true));
		addRequiredChildren();
		myManager.cacheHandler(getCacheKey(), tag, this);
		return getXmlTag();
	}

	public XmlElement getXmlElement()
	{
		return getParentStrategy().getXmlElement();
	}

	public boolean exists()
	{
		return getParentStrategy().isPhysical();
	}

	private DomParentStrategy getParentStrategy()
	{
		myParentStrategy = myParentStrategy.refreshStrategy(this);
		return myParentStrategy;
	}

	public XmlElement ensureXmlElementExists()
	{
		return ensureTagExists();
	}

	protected final XmlTag createChildTag(final EvaluatedXmlName tagName)
	{
		final String localName = tagName.getXmlName().getLocalName();
		if(localName.contains(":"))
		{
			try
			{
				return XmlElementFactory.getInstance(myManager.getProject()).createTagFromText("<" + localName + "/>");
			}
			catch(consulo.language.util.IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}

		final XmlElement element = getXmlElement();
		assert element != null;
		return getXmlTag().createChildTag(localName, tagName.getNamespace(element, getFile()), null, false);
	}

	public final boolean isValid()
	{
		return checkValidity() == null;
	}

	String toStringEx()
	{
		return myType.toString() + " @" + hashCode() + "&handler=" + super.toString() + "&cd=" + myChildDescription + "&ps=" + myParentStrategy;
	}

	@Nullable
	protected String checkValidity()
	{
		ProgressManager.checkCanceled();
		final DomParentStrategy parentStrategy = getParentStrategy();
		String error = parentStrategy.checkValidity();
		if(error != null)
		{
			return "Strategy: " + error;
		}

		final long modCount = myManager.getPsiModificationCount();
		if(myLastModCount == modCount)
		{
			return null;
		}

		final XmlElement xmlElement = parentStrategy.getXmlElement();
		if(xmlElement != null)
		{
			final DomInvocationHandler actual = myManager.getDomHandler(xmlElement);
			if(!equals(actual))
			{
				return "element changed: " + this.toStringEx() + "!=" + (actual == null ? null : actual.toStringEx());
			}
			myLastModCount = modCount;
			return null;
		}

		final DomInvocationHandler parent = getParentHandler();
		if(parent == null)
		{
			return "no parent: " + getDomElementType();
		}

		error = parent.checkValidity();
		if(error != null)
		{
			return "parent: " + error;
		}

		myLastModCount = modCount;
		return null;
	}


	@Nonnull
	public final DomGenericInfoEx getGenericInfo()
	{
		return myGenericInfo;
	}

	protected abstract void undefineInternal();

	public final void undefine()
	{
		undefineInternal();
	}

	protected final void deleteTag(final XmlTag tag)
	{
		final boolean changing = myManager.setChanging(true);
		try
		{
			tag.delete();
		}
		catch(consulo.language.util.IncorrectOperationException e)
		{
			LOG.error(e);
		}
		finally
		{
			myManager.setChanging(changing);
		}
	}

	protected final void fireUndefinedEvent()
	{
		myManager.fireEvent(new DomEvent(getProxy(), false));
	}

	protected abstract XmlTag setEmptyXmlTag();

	protected void addRequiredChildren()
	{
		for(final AbstractDomChildrenDescription description : getGenericInfo().getChildrenDescriptions())
		{
			if(description instanceof DomAttributeChildDescription)
			{
				final Required required = description.getAnnotation(Required.class);

				if(required != null && required.value())
				{
					description.getValues(getProxy()).get(0).ensureXmlElementExists();
				}
			}
			else if(description instanceof DomFixedChildDescription)
			{
				final DomFixedChildDescription childDescription = (DomFixedChildDescription) description;
				List<? extends DomElement> values = null;
				final int count = childDescription.getCount();
				for(int i = 0; i < count; i++)
				{
					final Required required = childDescription.getAnnotation(i, Required.class);
					if(required != null && required.value())
					{
						if(values == null)
						{
							values = description.getValues(getProxy());
						}
						values.get(i).ensureTagExists();
					}
				}
			}
		}
	}

	@Nonnull
	public final String getXmlElementName()
	{
		return myTagName.getXmlName().getLocalName();
	}

	@Nonnull
	public final EvaluatedXmlName getXmlName()
	{
		return myTagName;
	}

	public void accept(final DomElementVisitor visitor)
	{
		ProgressManager.checkCanceled();
		myManager.getApplicationComponent().getVisitorDescription(visitor.getClass()).acceptElement(visitor, getProxy());
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	public void acceptChildren(DomElementVisitor visitor)
	{
		ProgressManager.checkCanceled();
		final DomElement element = getProxy();
		List<? extends AbstractDomChildrenDescription> descriptions = getGenericInfo().getChildrenDescriptions();
		for(int i = 0, descriptionsSize = descriptions.size(); i < descriptionsSize; i++)
		{
			AbstractDomChildrenDescription description = descriptions.get(i);
			List<? extends DomElement> values = description.getValues(element);
			for(int j = 0, valuesSize = values.size(); j < valuesSize; j++)
			{
				DomElement value = values.get(j);
				value.accept(visitor);
			}
		}
	}

	@Nonnull
	protected final Converter getScalarConverter()
	{
		Converter converter = myScalarConverter;
		if(converter == null)
		{
			converter = myScalarConverter = createConverter(ourGetValue);
		}
		return converter;
	}

	@Nonnull
	private Converter createConverter(final JavaMethod method)
	{
		final Type returnType = method.getGenericReturnType();
		final Type type = returnType == void.class ? method.getGenericParameterTypes()[0] : returnType;
		final Class parameter = ReflectionUtil.substituteGenericType(type, myType);
		if(parameter == null)
		{
			LOG.error(type + " " + myType);
		}
		Converter converter = getConverter(new AnnotatedElement()
		{
			@Override
			public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
			{
				return myInvocationCache.getMethodAnnotation(method, annotationClass);
			}
		}, parameter);
		if(converter == null && type instanceof TypeVariable)
		{
			converter = getConverter(this, DomUtil.getGenericValueParameter(myType));
		}
		if(converter == null)
		{
			converter = myManager.getConverterManager().getConverterByClass(parameter);
		}
		if(converter == null)
		{
			throw new AssertionError("No converter specified: String<->" + parameter.getName() + "; method=" + method + "; place=" + myChildDescription);
		}
		return converter;
	}

	public final T getChildDescription()
	{
		return myChildDescription;
	}

	@Nullable
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass)
	{
		final AnnotatedElement childDescription = getChildDescription();
		if(childDescription != null)
		{
			final T annotation = childDescription.getAnnotation(annotationClass);
			if(annotation != null)
			{
				return annotation;
			}
		}

		return getClassAnnotation(annotationClass);
	}

	protected <T extends Annotation> T getClassAnnotation(Class<T> annotationClass)
	{
		return myInvocationCache.getClassAnnotation(annotationClass);
	}

	@Nullable
	private Converter getConverter(final AnnotatedElement annotationProvider,
								   Class parameter)
	{
		final Resolve resolveAnnotation = annotationProvider.getAnnotation(Resolve.class);
		if(resolveAnnotation != null)
		{
			final Class<? extends DomElement> aClass = resolveAnnotation.value();
			if(!DomElement.class.equals(aClass))
			{
				return DomResolveConverter.createConverter(aClass);
			}
			else
			{
				LOG.assertTrue(parameter != null, "You should specify @Resolve#value() parameter");
				return DomResolveConverter.createConverter(parameter);
			}
		}

		final ConverterManager converterManager = myManager.getConverterManager();
		Convert convertAnnotation = annotationProvider.getAnnotation(Convert.class);
		if(convertAnnotation != null)
		{
			if(convertAnnotation instanceof ConvertAnnotationImpl)
			{
				return ((ConvertAnnotationImpl) convertAnnotation).getConverter();
			}
			return converterManager.getConverterInstance(convertAnnotation.value());
		}

		return null;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public final DomElement getProxy()
	{
		if(myProxy == null)
		{
			final Class<?> rawType = getRawType();
			Class<? extends DomElement> implementation = getManager().getApplicationComponent().getImplementation(rawType);
			final boolean isInterface = rawType.isInterface();
			if(implementation == null && !isInterface)
			{
				implementation = (Class<? extends DomElement>) rawType;
			}
			Class[] interfaces = ArrayUtil.append(isInterface ? new Class[]{rawType} : ArrayUtil.EMPTY_CLASS_ARRAY, InvocationHandlerOwner.class);
			myProxy = AdvancedProxyBuilder.create(implementation).withInvocationHandler(this).withInterfaces(interfaces).build();
		}
		return myProxy;
	}

	@Nonnull
	public final XmlFile getFile()
	{
		return getParentStrategy().getContainingFile(this);
	}

	@Nonnull
	public DomNameStrategy getNameStrategy()
	{
		final Class<?> rawType = getRawType();
		final DomNameStrategy strategy = DomImplUtil.getDomNameStrategy(rawType, isAttribute());
		if(strategy != null)
		{
			return strategy;
		}
		final DomInvocationHandler handler = getParentHandler();
		return handler == null ? DomNameStrategy.HYPHEN_STRATEGY : handler.getNameStrategy();
	}

	protected boolean isAttribute()
	{
		return false;
	}

	@Nonnull
	public ElementPresentation getPresentation()
	{
		ElementPresentationTemplate template = getChildDescription().getPresentationTemplate();
		if(template != null)
		{
			return template.createPresentation(getProxy());
		}
		return new ElementPresentation()
		{
			public String getElementName()
			{
				return ElementPresentationManager.getElementName(getProxy());
			}

			public String getTypeName()
			{
				return ElementPresentationManager.getTypeNameForObject(getProxy());
			}

			public Image getIcon()
			{
				return ElementPresentationManager.getIcon(getProxy());
			}
		};
	}

	public final GlobalSearchScope getResolveScope()
	{
		return DomUtil.getFile(this).getResolveScope();
	}

	private static <T extends DomElement> T _getParentOfType(Class<T> requiredClass, DomElement element)
	{
		while(element != null && !requiredClass.isInstance(element))
		{
			element = element.getParent();
		}
		return (T) element;
	}

	public final <T extends DomElement> T getParentOfType(Class<T> requiredClass, boolean strict)
	{
		return _getParentOfType(requiredClass, strict ? getParent() : getProxy());
	}

	@Nonnull
	final IndexedElementInvocationHandler getFixedChild(final Pair<FixedChildDescriptionImpl, Integer> info)
	{
		final FixedChildDescriptionImpl description = info.first;
		XmlName xmlName = description.getXmlName();
		final EvaluatedXmlName evaluatedXmlName = createEvaluatedXmlName(xmlName);
		if(myStub != null && description.isStubbed())
		{
			List<DomStub> stubs = myStub.getChildrenByName(xmlName.getLocalName(), xmlName.getNamespaceKey());
			DomStub stub = stubs.isEmpty() ? null : stubs.get(0);
			DomParentStrategy strategy = stub == null ? new StubParentStrategy.Empty(myStub) : new StubParentStrategy(stub);
			return new IndexedElementInvocationHandler(evaluatedXmlName, description, 0, strategy, myManager, (ElementStub) stub);
		}
		final XmlTag tag = getXmlTag();
		final int index = info.second;
		if(tag != null)
		{
			if(!tag.isValid())
			{
				throw new PsiInvalidElementAccessException(tag);
			}
			final XmlTag[] subTags = tag.getSubTags();
			for(int i = 0, subTagsLength = subTags.length; i < subTagsLength; i++)
			{
				XmlTag xmlTag = subTags[i];
				if(!xmlTag.isValid())
				{
					throw new PsiInvalidElementAccessException(xmlTag,
							"invalid children of valid tag: " + tag.getText() + "; subtag=" + xmlTag + "; index=" + i);
				}
			}
			final List<XmlTag> tags = DomImplUtil.findSubTags(subTags, evaluatedXmlName, getFile());
			if(tags.size() > index)
			{
				final XmlTag child = tags.get(index);
				final IndexedElementInvocationHandler semElement = myManager.getSemService().getSemElement(DomManagerImpl.DOM_INDEXED_HANDLER_KEY, child);
				if(semElement == null)
				{
					final IndexedElementInvocationHandler take2 = myManager.getSemService().getSemElement(DomManagerImpl.DOM_INDEXED_HANDLER_KEY, child);
					throw new AssertionError("No DOM at XML. Parent=" + tag + "; child=" + child + "; index=" + index + "; second attempt=" + take2);

				}
				return semElement;
			}
		}
		return new IndexedElementInvocationHandler(evaluatedXmlName, description, index, new VirtualDomParentStrategy(this), myManager, null);
	}

	@Nonnull
	final AttributeChildInvocationHandler getAttributeChild(final AttributeChildDescriptionImpl description)
	{
		final EvaluatedXmlName evaluatedXmlName = createEvaluatedXmlName(description.getXmlName());
		if(myStub != null && description.isStubbed())
		{
			AttributeStub stub = myStub.getAttributeStub(description.getXmlName());
			StubParentStrategy strategy = StubParentStrategy.createAttributeStrategy(stub, myStub);
			return new AttributeChildInvocationHandler(evaluatedXmlName, description, myManager, strategy, stub);
		}
		final XmlTag tag = getXmlTag();

		if(tag != null)
		{
			// TODO: this seems ugly
			String ns = evaluatedXmlName.getNamespace(tag, getFile());
			final XmlAttribute attribute = tag.getAttribute(description.getXmlName().getLocalName(), ns.equals(tag.getNamespace()) ? null : ns);

			if(attribute != null)
			{
				LOG.assertTrue(attribute.isValid());
				return myManager.getSemService().getSemElement(DomManagerImpl.DOM_ATTRIBUTE_HANDLER_KEY, attribute);
			}
		}
		return new AttributeChildInvocationHandler(evaluatedXmlName, description, myManager, new VirtualDomParentStrategy(this), null);
	}

	@Nullable
	public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		try
		{
			if(method.equals(InvocationHandlerOwner.METHOD))
			{
				return this;
			}

			return findInvocation(method).invoke(this, args);
		}
		catch(InvocationTargetException ex)
		{
			throw ex.getTargetException();
		}
	}

	@Nonnull
	private Invocation findInvocation(Method method)
	{
		Invocation invocation = myAccessorInvocations.get(method);
		if(invocation != null)
		{
			return invocation;
		}

		invocation = myInvocationCache.getInvocation(method);
		if(invocation != null)
		{
			return invocation;
		}

		JavaMethod javaMethod = myInvocationCache.getInternedMethod(method);
		invocation = myGenericInfo.createInvocation(javaMethod);
		if(invocation != null)
		{
			myInvocationCache.putInvocation(method, invocation);
			return invocation;
		}

		if(myInvocationCache.isTagValueGetter(javaMethod))
		{
			invocation = new GetInvocation(createConverter(javaMethod));
		}
		else if(myInvocationCache.isTagValueSetter(javaMethod))
		{
			invocation = new SetInvocation(createConverter(javaMethod));
		}
		else
		{
			throw new RuntimeException("No implementation for method " + method.toString() + " in class " + myType);
		}
		myAccessorInvocations = myAccessorInvocations.plus(method, invocation);
		return invocation;
	}

	private static void setTagValue(final XmlTag tag, final String value)
	{
		tag.getValue().setText(value);
	}

	private static String getTagValue(final XmlTag tag)
	{
		return tag.getValue().getTrimmedText();
	}

	public final String toString()
	{
		if(ReflectionUtil.isAssignable(GenericValue.class, getRawType()))
		{
			return ((GenericValue) getProxy()).getStringValue();
		}
		return myType.toString() + " @" + hashCode();
	}

	protected final Class<?> getRawType()
	{
		return ReflectionUtil.getRawType(myType);
	}

	@Nullable
	public XmlTag getXmlTag()
	{
		return (XmlTag) getXmlElement();
	}

	@Nullable
	protected XmlElement recomputeXmlElement(@Nonnull final DomInvocationHandler parentHandler)
	{
		return null;
	}

	protected final void detach()
	{
		setXmlElement(null);
	}

	final SemKey getCacheKey()
	{
		if(this instanceof AttributeChildInvocationHandler)
		{
			return DomManagerImpl.DOM_ATTRIBUTE_HANDLER_KEY;
		}
		if(this instanceof DomRootInvocationHandler)
		{
			return DomManagerImpl.DOM_HANDLER_KEY;
		}
		if(this instanceof IndexedElementInvocationHandler)
		{
			return DomManagerImpl.DOM_INDEXED_HANDLER_KEY;
		}

		if(getChildDescription() instanceof CustomDomChildrenDescription)
		{
			return DomManagerImpl.DOM_CUSTOM_HANDLER_KEY;
		}

		return DomManagerImpl.DOM_COLLECTION_HANDLER_KEY;
	}

	protected final void setXmlElement(final XmlElement element)
	{
		refreshGenericInfo(element != null && !isAttribute());
		myParentStrategy = element == null ? myParentStrategy.clearXmlElement() : myParentStrategy.setXmlElement(element);
	}

	private void refreshGenericInfo(final boolean dynamic)
	{
		final StaticGenericInfo staticInfo = myManager.getApplicationComponent().getStaticGenericInfo(myType);
		myGenericInfo = dynamic ? new DynamicGenericInfo(this, staticInfo) : staticInfo;
	}

	@Nonnull
	public final DomManagerImpl getManager()
	{
		return myManager;
	}

	public final DomElement addCollectionChild(final CollectionChildDescriptionImpl description, final Type type, int index) throws IncorrectOperationException
	{
		final EvaluatedXmlName name = createEvaluatedXmlName(description.getXmlName());
		final XmlTag tag = addEmptyTag(name, index);
		final CollectionElementInvocationHandler handler = new CollectionElementInvocationHandler(type, tag, description, this, null);
		myManager.fireEvent(new DomEvent(getProxy(), false));
		getManager().getTypeChooserManager().getTypeChooser(description.getType()).distinguishTag(tag, type);
		handler.addRequiredChildren();
		return handler.getProxy();
	}

	protected final void createFixedChildrenTags(EvaluatedXmlName tagName, FixedChildDescriptionImpl description, int count)
	{
		final XmlTag tag = ensureTagExists();
		final List<XmlTag> subTags = DomImplUtil.findSubTags(tag, tagName, getFile());
		if(subTags.size() < count)
		{
			getFixedChild(Pair.create(description, count - 1)).ensureTagExists();
		}
	}

	private XmlTag addEmptyTag(final EvaluatedXmlName tagName, int index) throws consulo.language.util.IncorrectOperationException
	{
		final XmlTag tag = ensureTagExists();
		final List<XmlTag> subTags = DomImplUtil.findSubTags(tag, tagName, getFile());
		if(subTags.size() < index)
		{
			index = subTags.size();
		}
		final boolean changing = myManager.setChanging(true);
		try
		{
			XmlTag newTag = createChildTag(tagName);
			if(index == 0)
			{
				if(subTags.isEmpty())
				{
					return (XmlTag) tag.add(newTag);
				}

				return (XmlTag) tag.addBefore(newTag, subTags.get(0));
			}

			return (XmlTag) tag.addAfter(newTag, subTags.get(index - 1));
		}
		finally
		{
			myManager.setChanging(changing);
		}
	}

	@Nonnull
	public final EvaluatedXmlName createEvaluatedXmlName(final XmlName xmlName)
	{
		return getXmlName().evaluateChildName(xmlName);
	}

	public List<? extends DomElement> getCollectionChildren(final AbstractCollectionChildDescription description, final Function<DomInvocationHandler, List<XmlTag>> tagsGetter)
	{
		if(myStub != null && description.isStubbed())
		{
			if(description instanceof DomChildDescriptionImpl)
			{
				XmlName xmlName = ((DomChildDescriptionImpl) description).getXmlName();
				List<DomStub> stubs = myStub.getChildrenByName(xmlName.getLocalName(), xmlName.getNamespaceKey());
				return ContainerUtil.map(stubs, stub -> stub.getOrCreateHandler((DomChildDescriptionImpl) description, myManager).getProxy());
			}
			else if(description instanceof CustomDomChildrenDescriptionImpl)
			{
				List<DomStub> stubs = myStub.getChildrenStubs();
				return ContainerUtil.mapNotNull(stubs, new Function<DomStub, DomElement>()
				{
					@Nullable
					@Override
					public DomElement apply(DomStub stub)
					{
						if(stub instanceof ElementStub && ((ElementStub) stub).isCustom())
						{
							EvaluatedXmlName name = new DummyEvaluatedXmlName(stub.getName(), "");
							return new CollectionElementInvocationHandler(name, (CustomDomChildrenDescriptionImpl) description, myManager, (ElementStub) stub).getProxy();
						}
						return null;
					}
				});
			}
		}
		XmlTag tag = getXmlTag();
		if(tag == null)
		{
			return Collections.emptyList();
		}

		final List<XmlTag> subTags = tagsGetter.apply(this);
		if(subTags.isEmpty())
		{
			return Collections.emptyList();
		}

		List<DomElement> elements = new ArrayList<DomElement>(subTags.size());
		for(XmlTag subTag : subTags)
		{
			final SemKey<? extends DomInvocationHandler> key = description instanceof CustomDomChildrenDescription ? DomManagerImpl.DOM_CUSTOM_HANDLER_KEY : DomManagerImpl.DOM_COLLECTION_HANDLER_KEY;
			final DomInvocationHandler semElement = myManager.getSemService().getSemElement(key, subTag);
			if(semElement == null)
			{
				myManager.getSemService().getSemElement(key, subTag);
				throw new AssertionError("No child for subTag '" + subTag.getName() + "' in tag '" + tag.getName() + "' using key " + key);
			}
			else
			{
				elements.add(semElement.getProxy());
			}
		}
		return Collections.unmodifiableList(elements);
	}

	private static class StableCopyFactory<T extends DomElement> implements Supplier<T>
	{
		private final SmartPsiElementPointer<XmlTag> myPointer;
		private final Type myType;
		private final Class<? extends DomInvocationHandler> myHandlerClass;

		public StableCopyFactory(final SmartPsiElementPointer<XmlTag> pointer,
								 final Type type, final Class<? extends DomInvocationHandler> aClass)
		{
			myPointer = pointer;
			myType = type;
			myHandlerClass = aClass;
		}

		public T get()
		{
			final XmlTag tag = myPointer.getElement();
			if(tag == null || !tag.isValid())
			{
				return null;
			}

			final DomElement element = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
			if(element == null || !element.getDomElementType().equals(myType))
			{
				return null;
			}

			final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(element);
			if(handler == null || !handler.getClass().equals(myHandlerClass))
			{
				return null;
			}

			return (T) element;
		}
	}

	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || !o.getClass().equals(getClass()))
		{
			return false;
		}

		final DomInvocationHandler that = (DomInvocationHandler) o;
		if(!myChildDescription.equals(that.myChildDescription))
		{
			return false;
		}
		if(!getParentStrategy().equals(that.getParentStrategy()))
		{
			return false;
		}

		return true;
	}

	public int hashCode()
	{
		return myChildDescription.hashCode();
	}
}

