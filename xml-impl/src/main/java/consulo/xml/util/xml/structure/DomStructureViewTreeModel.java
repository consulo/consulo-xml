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

package consulo.xml.util.xml.structure;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.xml.ide.structureView.impl.xml.XmlFileTreeElement;
import consulo.xml.ide.structureView.impl.xml.XmlStructureViewTreeModel;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * @author Gregory.Shrago
 */
public class DomStructureViewTreeModel extends XmlStructureViewTreeModel implements Disposable
{
	private final DomElementNavigationProvider myNavigationProvider;
	private final Function<DomElement, DomService.StructureViewMode> myDescriptor;

	public DomStructureViewTreeModel(
			@Nonnull XmlFile file,
			@Nonnull Function<DomElement, DomService.StructureViewMode> descriptor,
			@Nullable Editor editor)
	{
		this(file, DomElementsNavigationManager.getManager(file.getProject()).getDomElementsNavigateProvider(DomElementsNavigationManager
				.DEFAULT_PROVIDER_NAME), descriptor, editor);
	}

	public DomStructureViewTreeModel(
			@Nonnull XmlFile file,
			final DomElementNavigationProvider navigationProvider,
			@Nonnull Function<DomElement, DomService.StructureViewMode> descriptor,
			@Nullable Editor editor)
	{
		super(file, editor);
		myNavigationProvider = navigationProvider;
		myDescriptor = descriptor;
	}

	@Override
	@Nonnull
	public StructureViewTreeElement getRoot()
	{
		XmlFile myFile = getPsiFile();
		final DomFileElement<DomElement> fileElement = DomManager.getDomManager(myFile.getProject()).getFileElement(myFile, DomElement.class);
		return fileElement == null ? new XmlFileTreeElement(myFile) : new DomStructureTreeElement(fileElement.getRootElement().createStableCopy(),
				myDescriptor, myNavigationProvider);
	}

	protected DomElementNavigationProvider getNavigationProvider()
	{
		return myNavigationProvider;
	}
}
