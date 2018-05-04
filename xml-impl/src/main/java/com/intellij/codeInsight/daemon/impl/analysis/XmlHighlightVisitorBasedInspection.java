/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.analysis;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.Nls;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.GlobalInspectionUtil;
import com.intellij.codeInspection.GlobalSimpleInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ProblemDescriptionsProcessor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlRecursiveElementVisitor;

/**
 * @author yole
 */
public class XmlHighlightVisitorBasedInspection extends GlobalSimpleInspectionTool
{
	@Nonnull
	@Override
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.ERROR;
	}

	@Override
	public void checkFile(@Nonnull final PsiFile file,
			@Nonnull final InspectionManager manager,
			@Nonnull ProblemsHolder problemsHolder,
			@Nonnull final GlobalInspectionContext globalContext,
			@Nonnull final ProblemDescriptionsProcessor problemDescriptionsProcessor)
	{
		HighlightInfoHolder myHolder = new HighlightInfoHolder(file)
		{
			@Override
			public boolean add(@Nullable HighlightInfo info)
			{
				if(info != null)
				{
					GlobalInspectionUtil.createProblem(file, info, new TextRange(info.startOffset, info.endOffset), null, manager, problemDescriptionsProcessor, globalContext);
				}
				return true;
			}
		};
		final XmlHighlightVisitor highlightVisitor = new XmlHighlightVisitor();
		highlightVisitor.analyze(file, true, myHolder, new Runnable()
		{
			@Override
			public void run()
			{
				file.accept(new XmlRecursiveElementVisitor()
				{
					@Override
					public void visitElement(PsiElement element)
					{
						highlightVisitor.visit(element);
						super.visitElement(element);
					}
				});
			}
		});

	}

	@Nls
	@Nonnull
	@Override
	public String getGroupDisplayName()
	{
		return InspectionProfileEntry.GENERAL_GROUP_NAME;
	}

	@Nls
	@Nonnull
	@Override
	public String getDisplayName()
	{
		return "XML highlighting";
	}

	@Nonnull
	@Override
	public String getShortName()
	{
		return "XmlHighlighting";
	}
}
