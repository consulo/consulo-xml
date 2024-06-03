/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.codeInsight.daemon.impl.quickfix.FetchExtResourceAction;
import consulo.xml.impl.internal.ExternalResource;
import consulo.xml.impl.internal.StandardExternalResourceData;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ExternalResourcesRootsProvider extends IndexableSetContributor
{
	private record Cache(Set<String> urls)
	{
		private static final ExtensionPointCacheKey<StandardResourceProvider, Cache> CACHE_KEY = ExtensionPointCacheKey.create("ExternalResourcesRootsProvider#Cache", walker ->
		{
			StandardExternalResourceData data = StandardExternalResourceData.build(walker);

			Set<String> set = new HashSet<>();
			Set<ExternalResource> dirs = new HashSet<>();

			for(Map<String, ExternalResource> map : data.resources().values())
			{
				for(ExternalResource resource : map.values())
				{
					ExternalResource dir = new ExternalResource(resource.directoryName(), resource);

					if(dirs.add(dir))
					{
						String url = resource.getResourceUrl();
						if(url != null)
						{
							set.add(url.substring(0, url.lastIndexOf('/') + 1));
						}
					}
				}
			}
			return new Cache(set);
		});
	}

	private final Application myApplication;

	@Inject
	public ExternalResourcesRootsProvider(Application application)
	{
		myApplication = application;
	}

	@Nonnull
	@Override
	public Set<VirtualFile> getAdditionalRootsToIndex()
	{
		Cache cache = myApplication.getExtensionPoint(StandardResourceProvider.class).getOrBuildCache(Cache.CACHE_KEY);

		Set<VirtualFile> roots = new HashSet<>();
		for(String url : cache.urls())
		{
			VirtualFile file = VirtualFileUtil.findRelativeFile(url, null);
			if(file != null)
			{
				roots.add(file);
			}
		}

		String path = FetchExtResourceAction.getExternalResourcesPath();
		VirtualFile extResources = LocalFileSystem.getInstance().findFileByPath(path);
		ContainerUtil.addIfNotNull(roots, extResources);

		return roots;
	}
}
