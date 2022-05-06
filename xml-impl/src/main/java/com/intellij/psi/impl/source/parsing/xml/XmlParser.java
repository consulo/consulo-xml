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

/*
 * @author max
 */
package com.intellij.psi.impl.source.parsing.xml;

import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import consulo.language.parser.PsiBuilder;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.util.lang.function.TripleFunction;
import consulo.language.version.LanguageVersion;
import consulo.language.ast.IElementType;
import consulo.language.ast.LighterASTNode;
import consulo.language.ast.LighterASTTokenNode;
import consulo.language.impl.internal.parser.PsiBuilderImpl;
import consulo.language.parser.PsiParser;
import consulo.util.lang.ThreeState;
import consulo.util.lang.ref.Ref;

import javax.annotation.Nonnull;

public class XmlParser implements PsiParser
{
	// tries to match an old and new XmlTag by name
	private static final TripleFunction<ASTNode, LighterASTNode, FlyweightCapableTreeStructure<LighterASTNode>, ThreeState>
			REPARSE_XML_TAG_BY_NAME = (oldNode, newNode, structure) -> {
		if(oldNode instanceof XmlTag && newNode.getTokenType() == XmlElementType.XML_TAG)
		{
			String oldName = ((XmlTag) oldNode).getName();
			Ref<LighterASTNode[]> childrenRef = Ref.create(null);
			int count = structure.getChildren(newNode, childrenRef);
			if(count < 3)
			{
				return ThreeState.UNSURE;
			}
			LighterASTNode[] children = childrenRef.get();
			if(children[0].getTokenType() != XmlTokenType.XML_START_TAG_START)
			{
				return ThreeState.UNSURE;
			}
			if(children[1].getTokenType() != XmlTokenType.XML_NAME)
			{
				return ThreeState.UNSURE;
			}
			if(children[2].getTokenType() != XmlTokenType.XML_TAG_END)
			{
				return ThreeState.UNSURE;
			}
			LighterASTTokenNode name = (LighterASTTokenNode) children[1];
			CharSequence newName = name.getText();
			if(!oldName.equals(newName))
			{
				return ThreeState.NO;
			}
		}

		return ThreeState.UNSURE;
	};

	@Nonnull
	public ASTNode parse(@Nonnull final IElementType root, @Nonnull final PsiBuilder builder, @Nonnull LanguageVersion languageVersion)
	{
		builder.enforceCommentTokens(TokenSet.EMPTY);
		builder.putUserData(PsiBuilderImpl.CUSTOM_COMPARATOR, REPARSE_XML_TAG_BY_NAME);
		final PsiBuilder.Marker file = builder.mark();
		new XmlParsing(builder).parseDocument();
		file.done(root);
		return builder.getTreeBuilt();
	}
}
