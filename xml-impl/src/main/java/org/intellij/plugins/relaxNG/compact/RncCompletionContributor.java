/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.TailType;
import consulo.language.editor.completion.lookup.TailTypeDecorator;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ArrayUtil;
import org.intellij.plugins.relaxNG.compact.psi.RncDecl;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;

import jakarta.annotation.Nonnull;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static consulo.language.pattern.StandardPatterns.and;
import static consulo.language.pattern.StandardPatterns.not;

/**
 * @author Dennis.Ushakov
 */
@ExtensionImpl
public class RncCompletionContributor extends CompletionContributor
{
	private static final ElementPattern TOP_LEVEL = not(psiElement().inside(psiElement(RncGrammar.class).inside(true, psiElement(RncGrammar.class))));

	private static final PsiElementPattern DECL_PATTERN = psiElement().inside(psiElement(RncDecl.class));

	private static final PsiElementPattern DEFAULT_PATTERN = DECL_PATTERN.afterLeaf(psiElement().withText("default"));

	private static final ElementPattern DEFINE_PATTERN = and(psiElement().withParent(RncDefine.class), psiElement().afterLeafSkipping(psiElement(PsiWhiteSpace.class), psiElement().withText("=")));

	private static final String[] DECL_KEYWORDS = new String[]{
			"default",
			"namespace",
			"datatypes"
	};
	private static final String[] GRAMMAR_CONTENT_KEYWORDS = new String[]{
			"include",
			"div",
			"start"
	};
	private static final String[] PATTERN_KEYWORDS = new String[]{
			"attribute",
			"element",
			"grammar",
			"notAllowed",
			"text",
			"empty",
			"external",
			"parent",
			"list",
			"mixed"
	};


	public RncCompletionContributor()
	{
		CompletionProvider provider = new CompletionProvider()
		{
			@Override
			public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result)
			{
				String[] keywords = getKeywords(parameters.getPosition());
				for(String keyword : keywords)
				{
					result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(keyword).bold(), TailType.SPACE));
				}
			}
		};
		extend(null, psiElement().afterLeaf(PlatformPatterns.psiElement(RncTokenTypes.KEYWORD_DEFAULT)), provider);
		extend(null, psiElement().andNot(psiElement().inside(PlatformPatterns.psiElement(RncTokenTypes.LITERAL))).
				andNot(psiElement().afterLeaf(psiElement().withElementType(RncTokenTypes.KEYWORDS))), provider);
	}


	private static String[] getKeywords(PsiElement context)
	{
		final PsiElement next = PsiTreeUtil.skipSiblingsForward(context, PsiWhiteSpace.class);
		if(next != null && EscapeUtil.unescapeText(next).equals("="))
		{
			return new String[]{"start"};
		}

		if(DEFAULT_PATTERN.accepts(context))
		{
			return new String[]{"namespace"};
		}
		else if(DECL_PATTERN.accepts(context))
		{
			return ArrayUtil.EMPTY_STRING_ARRAY;
		}
		else if(context.getParent() instanceof RncDefine && context.getParent().getFirstChild() == context)
		{
			if(DEFINE_PATTERN.accepts(context))
			{
				return ArrayUtil.EMPTY_STRING_ARRAY;
			}
			if(TOP_LEVEL.accepts(context))
			{
				if(!afterPattern(context))
				{
					return ArrayUtil.mergeArrays(DECL_KEYWORDS, ArrayUtil.mergeArrays(GRAMMAR_CONTENT_KEYWORDS, PATTERN_KEYWORDS));
				}
			}
			return GRAMMAR_CONTENT_KEYWORDS;
		}
		return PATTERN_KEYWORDS;
	}

	private static boolean afterPattern(PsiElement context)
	{
		// TODO: recognize all patterns
		return PsiTreeUtil.getPrevSiblingOfType(context.getParent(), RncDefine.class) != null;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
