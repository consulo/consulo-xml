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
package com.intellij.codeInsight.daemon.impl.tagTreeHighlighting;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.breadcrumbs.BreadcrumbsPresentationProvider;
import com.intellij.ui.breadcrumbs.CrumbPresentation;
import com.intellij.xml.breadcrumbs.DefaultCrumbsPresentation;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlTagTreeBreadcrumbsPresentationProvider extends BreadcrumbsPresentationProvider
{
	private static boolean isMyContext(@Nonnull PsiElement deepestElement)
	{
		final PsiFile file = deepestElement.getContainingFile();
		if(file == null || !XmlTagTreeHighlightingUtil.isTagTreeHighlightingActive(file))
		{
			return false;
		}
		return true;
	}

	@Override
	public CrumbPresentation[] getCrumbPresentations(@Nonnull PsiElement[] elements)
	{
		if(elements.length == 0 || !isMyContext(elements[elements.length - 1]))
		{
			return null;
		}

		if(!XmlTagTreeHighlightingUtil.containsTagsWithSameName(elements))
		{
			return null;
		}

		final CrumbPresentation[] result = new CrumbPresentation[elements.length];
		final ColorValue[] baseColors = XmlTagTreeHighlightingUtil.getBaseColors();
		int index = 0;

		for(int i = result.length - 1; i >= 0; i--)
		{
			if(elements[i] instanceof XmlTag)
			{
				final ColorValue color = baseColors[index % baseColors.length];
				result[i] = new MyCrumbPresentation(color);
				index++;
			}
		}
		return result;
	}

	private static class MyCrumbPresentation extends DefaultCrumbsPresentation
	{
		private final ColorValue myColor;

		private MyCrumbPresentation(@Nullable ColorValue color)
		{
			myColor = color;
		}

		@Override
		public ColorValue getBackgroundColor(boolean selected, boolean hovered, boolean light)
		{
			final ColorValue baseColor = super.getBackgroundColor(selected, hovered, light);
			return baseColor == null ? XmlTagTreeHighlightingPass.toLineMarkerColor(0x92, myColor) : myColor != null ? XmlTagTreeHighlightingUtil.makeTransparent(myColor, baseColor, 0.1) : baseColor;
		}
	}
}
