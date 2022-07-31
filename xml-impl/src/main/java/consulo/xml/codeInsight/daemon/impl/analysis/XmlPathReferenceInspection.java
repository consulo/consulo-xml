/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.util.HtmlUtil;
import consulo.language.Language;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlDoctype;
import consulo.xml.psi.xml.XmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * Date: 9/3/12
 */
public abstract class XmlPathReferenceInspection extends XmlSuppressableInspectionTool
{
	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly)
	{
		return new XmlElementVisitor()
		{
			@Override
			public void visitXmlAttributeValue(XmlAttributeValue value)
			{
				checkRefs(value, holder, isOnTheFly);
			}

			@Override
			public void visitXmlDoctype(XmlDoctype xmlDoctype)
			{
				checkRefs(xmlDoctype, holder, isOnTheFly);
			}

			@Override
			public void visitXmlTag(XmlTag tag)
			{
				checkRefs(tag, holder, isOnTheFly);
			}
		};
	}

	private void checkRefs(PsiElement element, ProblemsHolder holder, boolean isOnTheFly)
	{
		PsiReference[] references = element.getReferences();
		for(PsiReference reference : references)
		{
			if(!XmlHighlightVisitor.isUrlReference(reference))
			{
				continue;
			}
			if(!needToCheckRef(reference))
			{
				continue;
			}
			boolean isHtml = HtmlUtil.isHtmlTagContainingFile(element);
			if(isHtml ^ isForHtml())
			{
				continue;
			}
			if(!isHtml && XmlHighlightVisitor.skipValidation(element))
			{
				continue;
			}
			if(XmlHighlightVisitor.hasBadResolve(reference, false))
			{
				holder.registerProblem(reference, ProblemsHolder.unresolvedReferenceMessage(reference),
						isHtml ? ProblemHighlightType.GENERIC_ERROR_OR_WARNING : ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
			}
		}
	}

	@Nullable
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}

	protected boolean needToCheckRef(PsiReference reference)
	{
		return true;
	}

	protected boolean isForHtml()
	{
		return false;
	}
}
