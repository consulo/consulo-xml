// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.xml.util.xml.structure;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.ide.impl.idea.ide.structureView.newStructureView.StructureViewComponent;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.util.concurrent.Promise;
import consulo.xml.language.psi.XmlElement;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.DomService;

import org.jspecify.annotations.Nullable;
import java.util.function.Function;

public class DomStructureViewBuilder extends TreeBasedStructureViewBuilder
{
	private final Function<DomElement, DomService.StructureViewMode> myDescriptor;
	private final XmlFile myFile;

	public DomStructureViewBuilder(final XmlFile file, final Function<DomElement, DomService.StructureViewMode> descriptor)
	{
		myFile = file;
		myDescriptor = descriptor;
	}

	@Override
	public StructureViewModel createStructureViewModel(@Nullable Editor editor)
	{
		return new DomStructureViewTreeModel(myFile, myDescriptor, editor);
	}

	@Override
	public StructureView createStructureView(final FileEditor fileEditor, final Project project)
	{
		return new StructureViewComponent(fileEditor, createStructureViewModel(fileEditor instanceof TextEditor ? ((TextEditor) fileEditor).getEditor() : null), project, true)
		{
			@Override
			public Promise<AbstractTreeNode> expandPathToElement(final Object element)
			{
				if(element instanceof XmlElement && ((XmlElement) element).isValid())
				{
					final XmlElement xmlElement = (XmlElement) element;
					XmlTag tag = PsiTreeUtil.getParentOfType(xmlElement, XmlTag.class, false);
					while(tag != null)
					{
						final DomElement domElement = DomManager.getDomManager(xmlElement.getProject()).getDomElement(tag);
						if(domElement != null)
						{
							for(DomElement curElement = domElement; curElement != null; curElement = curElement.getParent())
							{
								if(myDescriptor.apply(curElement) == DomService.StructureViewMode.SHOW)
								{
									return super.expandPathToElement(curElement.getXmlElement());
								}
							}
						}
						tag = PsiTreeUtil.getParentOfType(tag, XmlTag.class, true);
					}

				}
				return super.expandPathToElement(element);
			}
		};
	}
}