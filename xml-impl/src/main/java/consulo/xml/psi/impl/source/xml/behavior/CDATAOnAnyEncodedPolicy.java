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
package consulo.xml.psi.impl.source.xml.behavior;

import com.intellij.xml.util.XmlUtil;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.ASTFactory;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;

public class CDATAOnAnyEncodedPolicy extends DefaultXmlPsiPolicy
{
	public ASTNode encodeXmlTextContents(String displayText, PsiElement text)
	{
		final ASTNode firstChild = text.getNode().getFirstChildNode();
		boolean textAlreadyHasCDATA = firstChild != null && firstChild.getElementType() == XmlElementType.XML_CDATA;
		if((textAlreadyHasCDATA || XmlUtil.toCode(displayText)) && displayText.length() > 0)
		{
			final FileElement dummyParent = createCDATAElement(text.getManager(), SharedImplUtil.findCharTableByTree(text.getNode()), displayText);
			return dummyParent.getFirstChildNode();
		}
		else
		{
			return super.encodeXmlTextContents(displayText, text);
		}
	}

	@SuppressWarnings({"HardCodedStringLiteral"})
	public static FileElement createCDATAElement(final PsiManager manager, final CharTable charTableByTree, final String displayText)
	{
		final FileElement dummyParent = DummyHolderFactory.createHolder(manager, null, charTableByTree).getTreeElement();
		final CompositeElement cdata = ASTFactory.composite(XmlElementType.XML_CDATA);
		dummyParent.rawAddChildren(cdata);
		cdata.rawAddChildren(ASTFactory.leaf(XmlTokenType.XML_CDATA_START, "<![CDATA["));
		cdata.rawAddChildren(ASTFactory.leaf(XmlTokenType.XML_DATA_CHARACTERS, dummyParent.getCharTable().intern(displayText)));
		cdata.rawAddChildren(ASTFactory.leaf(XmlTokenType.XML_CDATA_END, "]]>"));

		CodeEditUtil.markGenerated(dummyParent);
		return dummyParent;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return Language.ANY;
	}
}
