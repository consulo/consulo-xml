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
package com.intellij.ide.structureView.impl.xml;

import com.intellij.lang.dtd.DTDLanguage;
import com.intellij.psi.xml.*;
import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.Sorter;
import consulo.language.editor.structureView.TextEditorBasedStructureViewModel;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class XmlStructureViewTreeModel extends TextEditorBasedStructureViewModel
{
	private static final Class[] CLASSES = {
			XmlTag.class,
			XmlFile.class,
			XmlEntityDecl.class,
			XmlElementDecl.class,
			XmlAttlistDecl.class,
			XmlConditionalSection.class
	};
	private static final Sorter[] SORTERS = {Sorter.ALPHA_SORTER};

	public XmlStructureViewTreeModel(@Nonnull XmlFile file, @Nullable Editor editor)
	{
		super(editor, file);
	}

	@Override
	@Nonnull
	public StructureViewTreeElement getRoot()
	{
		XmlFile myFile = getPsiFile();
		if(myFile.getLanguage() == DTDLanguage.INSTANCE)
		{
			return new DtdFileTreeElement(myFile);
		}
		return new XmlFileTreeElement(myFile);
	}

	@Override
	public boolean shouldEnterElement(final Object element)
	{
		return element instanceof XmlTag && ((XmlTag) element).getSubTags().length > 0;
	}

	@Override
	protected XmlFile getPsiFile()
	{
		return (XmlFile) super.getPsiFile();
	}

	@Override
	@Nonnull
	protected Class[] getSuitableClasses()
	{
		return CLASSES;
	}

	@Override
	public Object getCurrentEditorElement()
	{
		final Object editorElement = super.getCurrentEditorElement();
		if(editorElement instanceof XmlTag)
		{
			for(StructureViewExtension extension : StructureViewFactoryEx.getInstanceEx(getPsiFile().getProject()).getAllExtensions(XmlTag.class))
			{
				final Object element = extension.getCurrentEditorElement(getEditor(), (PsiElement) editorElement);
				if(element != null)
				{
					return element;
				}
			}
		}
		return editorElement;
	}

	@Override
	@Nonnull
	public Sorter[] getSorters()
	{
		return SORTERS;
	}
}