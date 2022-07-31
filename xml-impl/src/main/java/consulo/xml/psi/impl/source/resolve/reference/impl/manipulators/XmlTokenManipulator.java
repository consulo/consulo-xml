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
package consulo.xml.psi.impl.source.resolve.reference.impl.manipulators;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.ASTFactory;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;
import consulo.xml.psi.xml.XmlToken;

import javax.annotation.Nonnull;

/**
 * @author ven
 */
@ExtensionImpl
public class XmlTokenManipulator extends AbstractElementManipulator<XmlToken>
{
	public XmlToken handleContentChange(XmlToken xmlToken, TextRange range, String newContent) throws IncorrectOperationException
	{
		String oldText = xmlToken.getText();
		String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());
		IElementType tokenType = xmlToken.getTokenType();

		FileElement holder = DummyHolderFactory.createHolder(xmlToken.getManager(), null).getTreeElement();
		LeafElement leaf = ASTFactory.leaf(tokenType, holder.getCharTable().intern(newText));
		holder.rawAddChildren(leaf);
		return (XmlToken) xmlToken.replace(leaf.getPsi());
	}

	@Nonnull
	@Override
	public Class<XmlToken> getElementClass()
	{
		return XmlToken.class;
	}
}
