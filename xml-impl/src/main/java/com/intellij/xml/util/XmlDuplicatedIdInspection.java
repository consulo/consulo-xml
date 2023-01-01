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
package com.intellij.xml.util;

import com.intellij.xml.XmlBundle;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.*;
import consulo.xml.codeInsight.daemon.XmlErrorMessages;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlDuplicatedIdInspection extends XmlSuppressableInspectionTool implements UnfairLocalInspectionTool
{
	@Override
	public boolean runForWholeFile()
	{
		return true;
	}

	@Nullable
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}

	@Nonnull
	@Override
	public String getGroupDisplayName()
	{
		return XmlBundle.message("xml.inspections.group.name");
	}

	@Nonnull
	@Override
	public String getDisplayName()
	{
		return XmlBundle.message("xml.inspections.duplicate.id");
	}

	@Nonnull
	@Override
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.ERROR;
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
	}

	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly)
	{
		return new XmlElementVisitor()
		{
			@Override
			public void visitXmlAttributeValue(final XmlAttributeValue value)
			{
				if(value.getTextRange().isEmpty())
				{
					return;
				}
				final PsiFile file = value.getContainingFile();
				if(!(file instanceof XmlFile))
				{
					return;
				}
				PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
				if(baseFile != file && !(baseFile instanceof XmlFile))
				{
					return;
				}
				final XmlRefCountHolder refHolder = XmlRefCountHolder.getRefCountHolder(value);
				if(refHolder == null)
				{
					return;
				}

				final PsiElement parent = value.getParent();
				if(!(parent instanceof XmlAttribute))
				{
					return;
				}

				final XmlTag tag = (XmlTag) parent.getParent();
				if(tag == null)
				{
					return;
				}

				checkValue(value, (XmlFile) file, refHolder, tag, holder);
			}
		};
	}

	protected void checkValue(XmlAttributeValue value, XmlFile file, XmlRefCountHolder refHolder, XmlTag tag, ProblemsHolder holder)
	{
		if(refHolder.isValidatable(tag.getParent()) && refHolder.isDuplicateIdAttributeValue(value))
		{
			holder.registerProblem(value, XmlErrorMessages.message("duplicate.id.reference"), ProblemHighlightType.GENERIC_ERROR,
					ElementManipulators.getValueTextRange(value));
		}
	}
}
