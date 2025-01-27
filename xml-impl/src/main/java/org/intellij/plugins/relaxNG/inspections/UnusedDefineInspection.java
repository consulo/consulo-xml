/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.inspections;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.query.Query;
import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncDefineImpl;
import org.intellij.plugins.relaxNG.model.resolve.RelaxIncludeIndex;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * User: sweinreuter
 * Date: 26.07.2007
 */
@ExtensionImpl
public class UnusedDefineInspection extends BaseInspection
{
	@Override
	public boolean isEnabledByDefault()
	{
		return false;
	}

	@Override
	@Nls
	@Nonnull
	public String getDisplayName()
	{
		return "Unused Define";
	}

	@Override
	@NonNls
	@Nonnull
	public String getShortName()
	{
		return "UnusedDefine";
	}

	@Nonnull
	@Override
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.WARNING;
	}

	@Override
	@Nonnull
	public RncElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly)
	{
		return new MyElementVisitor(holder);
	}

	private static final class MyElementVisitor extends RncElementVisitor
	{
		private final ProblemsHolder myHolder;

		private final XmlElementVisitor myXmlVisitor = new XmlElementVisitor()
		{
			@Override
			public void visitXmlTag(XmlTag tag)
			{
				MyElementVisitor.this.visitXmlTag(tag);
			}
		};

		public MyElementVisitor(ProblemsHolder holder)
		{
			myHolder = holder;
		}

		@Override
		protected void superVisitElement(PsiElement element)
		{
			element.accept(myXmlVisitor);
		}

		@Override
		public void visitDefine(RncDefine pattern)
		{
			final RncGrammar grammar = PsiTreeUtil.getParentOfType(pattern, RncGrammar.class);
			final PsiFile file = pattern.getContainingFile();
			if(grammar != null)
			{
				if(processRncUsages(pattern, new LocalSearchScope(grammar)))
				{
					return;
				}
			}
			else
			{
				if(processRncUsages(pattern, new LocalSearchScope(file)))
				{
					return;
				}
			}

			final PsiElementProcessor.CollectElements<XmlFile> collector = new PsiElementProcessor.CollectElements<>();
			RelaxIncludeIndex.processBackwardDependencies((XmlFile) file, collector);

			if(processRncUsages(pattern, new LocalSearchScope(collector.toArray())))
			{
				return;
			}

			final ASTNode astNode = ((RncDefineImpl) pattern).getNameNode();
			myHolder.registerProblem(astNode.getPsi(), "Unreferenced define", ProblemHighlightType.LIKE_UNUSED_SYMBOL, new MyFix<>(pattern));
		}

		private static boolean processRncUsages(PsiElement tag, LocalSearchScope scope)
		{
			final Query<PsiReference> query = ReferencesSearch.search(tag, scope);
			for(PsiReference reference : query)
			{
				final PsiElement e = reference.getElement();
				final RncDefine t = PsiTreeUtil.getParentOfType(e, RncDefine.class, false);
				if(t == null || !PsiTreeUtil.isAncestor(tag, t, true))
				{
					return true;
				}
			}
			return false;
		}

		public void visitXmlTag(XmlTag tag)
		{
			final PsiFile file = tag.getContainingFile();
			if(file.getFileType() != XmlFileType.INSTANCE)
			{
				return;
			}
			if(!tag.getLocalName().equals("define"))
			{
				return;
			}
			if(!tag.getNamespace().equals(ApplicationLoader.RNG_NAMESPACE))
			{
				return;
			}
			if(tag.getAttribute("combine") != null)
			{
				return; // ?
			}

			final XmlAttribute attr = tag.getAttribute("name");
			if(attr == null)
			{
				return;
			}

			final XmlAttributeValue value = attr.getValueElement();
			if(value == null)
			{
				return;
			}

			final String s = value.getValue();
			if(s == null || s.length() == 0)
			{
				return;
			}
			final PsiElement parent = value.getParent();
			if(!(parent instanceof XmlAttribute))
			{
				return;
			}
			if(!"name".equals(((XmlAttribute) parent).getName()))
			{
				return;
			}
			final PsiElement grandParent = parent.getParent();
			if(!(grandParent instanceof XmlTag))
			{
				return;
			}

			final DomElement element = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
			if(element == null)
			{
				return;
			}

			final RngGrammar rngGrammar = element.getParentOfType(RngGrammar.class, true);
			if(rngGrammar != null)
			{
				if(processUsages(tag, value, new LocalSearchScope(rngGrammar.getXmlTag())))
				{
					return;
				}
			}
			else
			{
				if(processUsages(tag, value, new LocalSearchScope(file)))
				{
					return;
				}
			}

			final PsiElementProcessor.CollectElements<XmlFile> collector = new PsiElementProcessor.CollectElements<>();
			RelaxIncludeIndex.processBackwardDependencies((XmlFile) file, collector);

			if(processUsages(tag, value, new LocalSearchScope(collector.toArray())))
			{
				return;
			}

			myHolder.registerProblem(value, "Unreferenced define", ProblemHighlightType.LIKE_UNUSED_SYMBOL, new MyFix<>(tag));
		}

		private static boolean processUsages(PsiElement tag, XmlAttributeValue value, LocalSearchScope scope)
		{
			final Query<PsiReference> query = ReferencesSearch.search(tag, scope, true);
			for(PsiReference reference : query)
			{
				final PsiElement e = reference.getElement();
				if(e != value)
				{
					final XmlTag t = PsiTreeUtil.getParentOfType(e, XmlTag.class);
					if(t != null && !PsiTreeUtil.isAncestor(tag, t, true))
					{
						return true;
					}
				}
			}
			return false;
		}

		private static class MyFix<T extends PsiElement> implements LocalQuickFix
		{
			private final T myTag;

			public MyFix(T tag)
			{
				myTag = tag;
			}

			@Override
			@Nonnull
			public String getFamilyName()
			{
				return "Remove define";
			}

			@Override
			public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
			{
				try
				{
					if(myTag.isValid())
					{
						myTag.delete();
					}
				}
				catch(IncorrectOperationException e)
				{
					Logger.getInstance(UnusedDefineInspection.class.getName()).error(e);
				}
			}
		}
	}
}
