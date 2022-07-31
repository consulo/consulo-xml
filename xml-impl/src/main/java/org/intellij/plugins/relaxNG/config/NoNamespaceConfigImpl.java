/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.relaxNG.config;

import consulo.annotation.component.ServiceImpl;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.language.editor.HectorComponentPanel;
import consulo.language.editor.HectorComponentPanelsProvider;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Singleton
@State(name = "NoNamespaceConfig.Mappings", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
class NoNamespaceConfigImpl extends NoNamespaceConfig implements PersistentStateComponent<NoNamespaceConfigImpl.Mappings>, Disposable
{
	private final Map<VirtualFilePointer, VirtualFilePointer> myMappings = new HashMap<>();
	private final Project myProject;

	@Inject
	NoNamespaceConfigImpl(Project project)
	{
		myProject = project;
	}

	private VirtualFilePointer getMappedPointer(@Nonnull PsiFile file)
	{
		final VirtualFile virtualFile = file.getVirtualFile();
		if(virtualFile == null)
		{
			return null;
		}

		final String url = virtualFile.getUrl();
		for(VirtualFilePointer pointer : myMappings.keySet())
		{
			if(url.equals(pointer.getUrl()))
			{
				return myMappings.get(pointer);
			}
		}
		return null;
	}

	@Override
	@Nullable
	public String getMapping(@Nonnull PsiFile file)
	{
		final VirtualFilePointer pointer = getMappedPointer(file);
		return pointer != null ? pointer.getUrl() : null;
	}

	@Override
	public VirtualFile getMappedFile(@Nonnull PsiFile file)
	{
		final VirtualFilePointer url = getMappedPointer(file);
		return url != null ? url.getFile() : null;
	}

	@Override
	public void setMapping(@Nonnull PsiFile file, String location)
	{
		final VirtualFile virtualFile = file.getVirtualFile();
		assert virtualFile != null;

		final String url = virtualFile.getUrl();
		final VirtualFilePointerManager manager = VirtualFilePointerManager.getInstance();
		for(VirtualFilePointer pointer : myMappings.keySet())
		{
			if(url.equals(pointer.getUrl()))
			{
				if(location == null)
				{
					myMappings.remove(pointer);
					return;
				}
				else if(!location.equals(myMappings.get(pointer).getUrl()))
				{
					myMappings.remove(pointer);
					myMappings.put(pointer, manager.create(location, myProject, null));
					return;
				}
			}
		}

		if(location != null)
		{
			myMappings.put(manager.create(url, myProject, null), manager.create(location, myProject, null));
		}
	}

	@Override
	public void dispose()
	{
		reset();
	}

	@Override
	public Mappings getState()
	{
		final HashMap<String, String> map = new HashMap<>();
		for(Map.Entry<VirtualFilePointer, VirtualFilePointer> entry : myMappings.entrySet())
		{
			map.put(entry.getKey().getUrl(), entry.getValue().getUrl());
		}
		return new Mappings(map);
	}

	@Override
	public void loadState(Mappings state)
	{
		reset();

		final VirtualFilePointerManager manager = VirtualFilePointerManager.getInstance();
		final Map<String, String> map = state.myMappings;
		for(String file : map.keySet())
		{
			myMappings.put(manager.create(file, myProject, null), manager.create(map.get(file), myProject, null));
		}
	}

	private void reset()
	{
		myMappings.clear();
	}

	@SuppressWarnings({
			"CanBeFinal",
			"UnusedDeclaration"
	})
	public static class Mappings
	{
		@MapAnnotation(surroundWithTag = false, entryTagName = "mapping", keyAttributeName = "file", valueAttributeName = "schema")
		public Map<String, String> myMappings;

		public Mappings()
		{
			myMappings = new HashMap<>();
		}

		Mappings(Map<String, String> map)
		{
			myMappings = map;
		}
	}

	public static class HectorProvider implements HectorComponentPanelsProvider
	{
		@Override
		@Nullable
		public HectorComponentPanel createConfigurable(@Nonnull PsiFile file)
		{
			if(file instanceof XmlFile)
			{
				try
				{
					final XmlTag rootTag = ((XmlFile) file).getDocument().getRootTag();
					if(rootTag.getNamespace().length() == 0)
					{
						return new NoNamespaceConfigPanel(getInstance(file.getProject()), file);
					}
				}
				catch(NullPointerException e)
				{
					return null;
				}
			}
			return null;
		}
	}
}
