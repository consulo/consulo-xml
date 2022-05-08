/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xml.index;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.xml.ide.highlighter.DTDFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.index.io.EnumeratorStringDescriptor;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlIndex<V> extends FileBasedIndexExtension<String, V>
{
	protected static GlobalSearchScope createFilter(final Project project)
	{
		final GlobalSearchScope projectScope = GlobalSearchScope.allScope(project);
		return new GlobalSearchScope(project)
		{
			@Override
			public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2)
			{
				return projectScope.compare(file1, file2);
			}

			@Override
			public boolean isSearchInModuleContent(@Nonnull consulo.module.Module aModule)
			{
				return true;
			}

			@Override
			public boolean contains(@Nonnull VirtualFile file)
			{
				final VirtualFile parent = file.getParent();
				return parent != null && (parent.getName().equals("standardSchemas") || projectScope.contains(file));
			}

			@Override
			public boolean isSearchInLibraries()
			{
				return true;
			}
		};
	}


	protected static VirtualFileFilter createFilter(@Nonnull final consulo.module.Module module)
	{

		final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
		return new VirtualFileFilter()
		{
			@Override
			public boolean accept(final VirtualFile file)
			{
				Module moduleForFile = fileIndex.getModuleForFile(file);
				if(moduleForFile != null)
				{ // in module content
					return module.equals(moduleForFile);
				}
				if(fileIndex.isInLibraryClasses(file))
				{
					List<OrderEntry> orderEntries = fileIndex.getOrderEntriesForFile(file);
					if(orderEntries.isEmpty())
					{
						return false;
					}
					for(OrderEntry orderEntry : orderEntries)
					{
						consulo.module.Module ownerModule = orderEntry.getOwnerModule();
						if(ownerModule.equals(module))
						{
							return true;
						}
					}
				}
				final VirtualFile parent = file.getParent();
				assert parent != null;
				return parent.getName().equals("standardSchemas");
			}
		};
	}

	@Override
	@Nonnull
	public KeyDescriptor<String> getKeyDescriptor()
	{
		return EnumeratorStringDescriptor.INSTANCE;
	}

	@Override
	@Nonnull
	public FileBasedIndex.InputFilter getInputFilter()
	{
		return new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE, DTDFileType.INSTANCE)
		{
			@Override
			public boolean acceptInput(@Nullable Project project, @Nonnull final VirtualFile file)
			{
				FileType fileType = file.getFileType();
				final String extension = file.getExtension();
				return XmlFileType.INSTANCE.equals(fileType) && "xsd".equals(extension) || DTDFileType.INSTANCE.equals(fileType) && "dtd".equals(extension);
			}
		};
	}

	@Override
	public boolean dependsOnFileContent()
	{
		return true;
	}

	@Override
	public int getVersion()
	{
		return 0;
	}
}
