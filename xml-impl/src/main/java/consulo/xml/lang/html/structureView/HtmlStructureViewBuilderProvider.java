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
package consulo.xml.lang.html.structureView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.xml.ide.structureView.xml.XmlStructureViewBuilderProvider;
import consulo.codeEditor.Editor;
import consulo.xml.psi.xml.XmlFile;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;

public class HtmlStructureViewBuilderProvider implements XmlStructureViewBuilderProvider
{
	@Override
	@Nullable
	public StructureViewBuilder createStructureViewBuilder(@Nonnull final XmlFile file)
	{
		if(file.getViewProvider().getVirtualFile().getFileType() != HtmlFileType.INSTANCE)
		{
			return null;
		}

		return new TreeBasedStructureViewBuilder()
		{
			@Override
			public boolean isRootNodeShown()
			{
				return false;
			}

			@Override
			@Nonnull
			public StructureViewModel createStructureViewModel(@Nullable Editor editor)
			{
				return new HtmlStructureViewTreeModel(file, editor);
			}
		};
	}
}