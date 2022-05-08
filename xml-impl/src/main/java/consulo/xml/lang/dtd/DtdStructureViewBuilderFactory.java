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

/*
 * @author max
 */
package consulo.xml.lang.dtd;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.xml.ide.structureView.impl.xml.XmlStructureViewTreeModel;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.fileEditor.structureView.StructureViewModel;

public class DtdStructureViewBuilderFactory implements PsiStructureViewFactory
{
	@Override
	@Nonnull
	public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile)
	{
		return new TreeBasedStructureViewBuilder()
		{
			@Override
			@Nonnull
			public StructureViewModel createStructureViewModel(@Nullable Editor editor)
			{
				return new XmlStructureViewTreeModel((XmlFile) psiFile, editor);
			}
		};
	}
}