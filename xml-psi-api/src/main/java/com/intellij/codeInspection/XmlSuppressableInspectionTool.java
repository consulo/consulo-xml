/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInspection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

public abstract class XmlSuppressableInspectionTool extends LocalInspectionTool implements BatchSuppressableTool
{
	@NonNls
	static final String ALL = "ALL";

	@Nonnull
	@Override
	public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element)
	{
		return getSuppressFixes(getID());
	}

	public static SuppressQuickFix[] getSuppressFixes(final String shortName)
	{
		final String id = HighlightDisplayKey.find(shortName).getID();
		return new SuppressQuickFix[]{
				new SuppressTagStatic(id),
				new SuppressForFile(id),
				new SuppressAllForFile()
		};
	}

	@Override
	public boolean isSuppressedFor(@Nonnull final PsiElement element)
	{
		return XmlSuppressionProvider.isSuppressed(element, getID());
	}

	public class SuppressTag extends SuppressTagStatic
	{
		public SuppressTag()
		{
			super(getID());
		}
	}

	public static class SuppressTagStatic implements SuppressQuickFix
	{
		private final String id;

		public SuppressTagStatic(@Nonnull String id)
		{
			this.id = id;
		}

		@Nonnull
		@Override
		public String getName()
		{
			return InspectionsBundle.message("xml.suppressable.for.tag.title");
		}

		@Override
		public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context)
		{
			return context.isValid();
		}

		@Override
		public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
		{
			PsiElement element = descriptor.getPsiElement();
			if(PsiTreeUtil.getParentOfType(element, XmlTag.class) == null)
			{
				return;
			}
			XmlSuppressionProvider.getProvider(element.getContainingFile()).suppressForTag(element, id);
		}

		@Override
		@Nonnull
		public String getFamilyName()
		{
			return getName();
		}
	}

	public static class SuppressForFile implements SuppressQuickFix
	{
		private final String myInspectionId;

		public SuppressForFile(@Nonnull String inspectionId)
		{
			myInspectionId = inspectionId;
		}

		@Nonnull
		@Override
		public String getName()
		{
			return InspectionsBundle.message("xml.suppressable.for.file.title");
		}

		@Override
		public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
		{
			PsiElement element = descriptor.getPsiElement();
			if(element == null || !element.isValid() || !(element.getContainingFile() instanceof XmlFile))
			{
				return;
			}
			XmlSuppressionProvider.getProvider(element.getContainingFile()).suppressForFile(element, myInspectionId);
		}

		@Override
		public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context)
		{
			return context.isValid();
		}

		@Override
		@Nonnull
		public String getFamilyName()
		{
			return getName();
		}
	}

	public static class SuppressAllForFile extends SuppressForFile
	{
		public SuppressAllForFile()
		{
			super(ALL);
		}

		@Nonnull
		@Override
		public String getName()
		{
			return InspectionsBundle.message("xml.suppressable.all.for.file.title");
		}
	}
}
