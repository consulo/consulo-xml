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
package com.intellij.javaee;

import com.intellij.codeInsight.daemon.impl.quickfix.FetchExtResourceAction;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.ide.impl.idea.openapi.util.NotNullLazyValue;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class ExternalResourcesRootsProvider extends IndexableSetContributor
{
	private final NotNullLazyValue<Set<String>> myStandardResources = new NotNullLazyValue<Set<String>>()
	{
		@Nonnull
		@Override
		protected Set<String> compute()
		{
			ExternalResourceManagerExImpl manager = (ExternalResourceManagerExImpl) ExternalResourceManager.getInstance();
			Set<ExternalResourceManagerExImpl.Resource> dirs = new HashSet<>();
			Set<String> set = new HashSet<>();
			for(Map<String, ExternalResourceManagerExImpl.Resource> map : manager.getStandardResources())
			{
				for(ExternalResourceManagerExImpl.Resource resource : map.values())
				{
					ExternalResourceManagerExImpl.Resource dir = new ExternalResourceManagerExImpl.Resource(resource.directoryName(), resource);

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
			return set;
		}
	};

	@Nonnull
	@Override
	public Set<VirtualFile> getAdditionalRootsToIndex()
	{
		Set<VirtualFile> roots = new HashSet<>();
		for(String url : myStandardResources.getValue())
		{
			VirtualFile file = VfsUtilCore.findRelativeFile(url, null);
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
