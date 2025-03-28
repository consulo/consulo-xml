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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.ide.ServiceManager;
import consulo.util.collection.FactoryMap;
import consulo.util.lang.ReflectionAssignabilityCache;
import consulo.xml.dom.DomElementImplementationProvider;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementVisitor;
import consulo.xml.util.xml.DomFileDescription;
import consulo.xml.util.xml.TypeChooserManager;
import consulo.xml.util.xml.highlighting.DomElementsAnnotator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author peter
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DomApplicationComponent
{
	private final Map<String, Set<DomFileDescription>> myRootTagName2FileDescription = FactoryMap.create(k -> new HashSet<DomFileDescription>());
	private final Set<DomFileDescription> myAcceptingOtherRootTagNamesDescriptions = new HashSet<DomFileDescription>();
	private final ImplementationClassCache<DomElementImplementationProvider> myCachedImplementationClasses = new ImplementationClassCache<>(DomElementImplementationProvider.class);
	private final TypeChooserManager myTypeChooserManager = new TypeChooserManager();
	final ReflectionAssignabilityCache assignabilityCache = new ReflectionAssignabilityCache();
	private final Map<Class, DomElementsAnnotator> myClass2Annotator = ConcurrentFactoryMap.createMap(key ->
	{
		final DomFileDescription desc = findFileDescription(key);
		return desc == null ? null : desc.createAnnotator();
	});

	private final SofterCache<Type, StaticGenericInfo> myGenericInfos = SofterCache.create(type -> new StaticGenericInfo(type));
	private final SofterCache<Class, InvocationCache> myInvocationCaches = SofterCache.create(key -> new InvocationCache(key));
	private final Map<Class<? extends DomElementVisitor>, VisitorDescription> myVisitorDescriptions = ConcurrentFactoryMap.createMap(key -> new VisitorDescription(key));

	@Inject
	public DomApplicationComponent(Application application)
	{
		application.getExtensionPoint(DomFileDescription.class).forEachExtensionSafe(this::registerFileDescription);
	}

	public static DomApplicationComponent getInstance()
	{
		return ServiceManager.getService(DomApplicationComponent.class);
	}

	public int getCumulativeVersion()
	{
		int result = 0;
		for(DomFileDescription description : getAllFileDescriptions())
		{
			result += description.getVersion();
			result += description.getRootTagName().hashCode(); // so that a plugin enabling/disabling could trigger the reindexing
		}
		return result;
	}

	public final synchronized Set<DomFileDescription> getFileDescriptions(String rootTagName)
	{
		return myRootTagName2FileDescription.get(rootTagName);
	}

	public final synchronized Set<DomFileDescription> getAcceptingOtherRootTagNameDescriptions()
	{
		return myAcceptingOtherRootTagNamesDescriptions;
	}

	public final synchronized void registerFileDescription(final DomFileDescription description)
	{
		myRootTagName2FileDescription.get(description.getRootTagName()).add(description);
		if(description.acceptsOtherRootTagNames())
		{
			myAcceptingOtherRootTagNamesDescriptions.add(description);
		}

		myTypeChooserManager.copyFrom(description.getTypeChooserManager());
	}

	public synchronized List<DomFileDescription> getAllFileDescriptions()
	{
		final List<DomFileDescription> result = new ArrayList<>();
		for(Set<DomFileDescription> descriptions : myRootTagName2FileDescription.values())
		{
			result.addAll(descriptions);
		}
		result.addAll(myAcceptingOtherRootTagNamesDescriptions);
		return result;
	}

	@Nullable
	private synchronized DomFileDescription findFileDescription(Class rootElementClass)
	{
		for(Set<DomFileDescription> descriptions : myRootTagName2FileDescription.values())
		{
			for(DomFileDescription description : descriptions)
			{
				if(description.getRootElementClass() == rootElementClass)
				{
					return description;
				}
			}
		}

		for(DomFileDescription description : myAcceptingOtherRootTagNamesDescriptions)
		{
			if(description.getRootElementClass() == rootElementClass)
			{
				return description;
			}
		}
		return null;
	}

	public DomElementsAnnotator getAnnotator(Class rootElementClass)
	{
		return myClass2Annotator.get(rootElementClass);
	}

	@Nullable
	final Class<? extends DomElement> getImplementation(final Class concreteInterface)
	{
		//noinspection unchecked
		DomElementImplementationProvider provider = myCachedImplementationClasses.get(concreteInterface);
		return provider == null ? null : provider.getImplementationClass();
	}

	public TypeChooserManager getTypeChooserManager()
	{
		return myTypeChooserManager;
	}

	public final StaticGenericInfo getStaticGenericInfo(final Type type)
	{
		return myGenericInfos.getCachedValue(type);
	}

	final InvocationCache getInvocationCache(final Class type)
	{
		return myInvocationCaches.getCachedValue(type);
	}

	public final VisitorDescription getVisitorDescription(Class<? extends DomElementVisitor> aClass)
	{
		return myVisitorDescriptions.get(aClass);
	}

}
