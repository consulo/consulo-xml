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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.tree.NodeProvider;
import consulo.ide.IdeBundle;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.xml.ide.structureView.impl.xml.XmlStructureViewTreeModel;
import consulo.fileEditor.structureView.tree.ActionPresentation;
import consulo.fileEditor.structureView.tree.ActionPresentationData;
import consulo.fileEditor.structureView.tree.Sorter;
import consulo.fileEditor.structureView.tree.SorterUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.TreeStructureUtil;
import consulo.ui.ex.PlaceHolder;

class HtmlStructureViewTreeModel extends XmlStructureViewTreeModel implements PlaceHolder<String>
{

	private final Collection<NodeProvider> myNodeProviders;
	private String myStructureViewPlace;

	private static final Sorter HTML_ALPHA_SORTER = new Sorter()
	{
		@Nonnull
		@Override
		public Comparator getComparator()
		{
			return new Comparator()
			{
				@Override
				public int compare(Object o1, Object o2)
				{
					String s1 = SorterUtil.getStringPresentation(o1);
					String s2 = SorterUtil.getStringPresentation(o2);

					if(isTagPresentation(s1, "head") && isTagPresentation(s2, "body"))
					{
						return -1;
					}
					if(isTagPresentation(s1, "body") && isTagPresentation(s2, "head"))
					{
						return 1;
					}

					return s1.compareToIgnoreCase(s2);
				}

				private boolean isTagPresentation(final String presentation, final String tagName)
				{
					// "head", "head#id", "head.cls"
					final String lowerCased = presentation.toLowerCase();
					return lowerCased.startsWith(tagName) && (lowerCased.length() == tagName.length() || !Character.isLetter(lowerCased.charAt
							(tagName.length())));
				}
			};
		}

		@Override
		public boolean isVisible()
		{
			return true;
		}

		public String toString()
		{
			return getName();
		}

		@Override
		@Nonnull
		public ActionPresentation getPresentation()
		{
			return new ActionPresentationData(IdeBundle.message("action.sort.alphabetically"), IdeBundle.message("action.sort.alphabetically"),
					AllIcons.ObjectBrowser.Sorted);
		}

		@Override
		@Nonnull
		public String getName()
		{
			return ALPHA_SORTER_ID;
		}
	};

	private static final Sorter[] ourSorters = {HTML_ALPHA_SORTER};

	public HtmlStructureViewTreeModel(final XmlFile file, @Nullable Editor editor)
	{
		super(file, editor);

		myNodeProviders = Arrays.<NodeProvider>asList(new Html5SectionsNodeProvider());
	}

	@Override
	public void setPlace(@Nonnull final String place)
	{
		myStructureViewPlace = place;
	}

	@Override
	public String getPlace()
	{
		return myStructureViewPlace;
	}

	@Override
	@Nonnull
	public Sorter[] getSorters()
	{
		if(TreeStructureUtil.isInStructureViewPopup(this))
		{
			return Sorter.EMPTY_ARRAY;  // because in popup there's no option to disable sorter
		}

		return ourSorters;
	}

	@Override
	@Nonnull
	public Collection<NodeProvider> getNodeProviders()
	{
		return myNodeProviders;
	}

	@Override
	@Nonnull
	public StructureViewTreeElement getRoot()
	{
		return new HtmlFileTreeElement(TreeStructureUtil.isInStructureViewPopup(this), (XmlFile) getPsiFile());
	}
}
