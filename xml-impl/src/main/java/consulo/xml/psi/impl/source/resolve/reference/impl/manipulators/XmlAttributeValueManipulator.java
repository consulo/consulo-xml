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
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.Factory;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.psi.CheckUtil;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.psi.PsiElement;
import consulo.language.util.CharTable;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;

/**
 * @author ik
 * @since 2004-01-06
 */
@ExtensionImpl
public class XmlAttributeValueManipulator extends AbstractElementManipulator<XmlAttributeValue>
{
	private static final Logger LOG = Logger.getInstance(XmlAttributeValueManipulator.class);

	public XmlAttributeValue handleContentChange(XmlAttributeValue element, TextRange range, String newContent) throws IncorrectOperationException
	{
		return handleContentChange(element, range, newContent, XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN);
	}

	public static <T extends PsiElement> T handleContentChange(T element,
															   TextRange range,
															   String newContent,
															   final IElementType tokenType)
	{
		CheckUtil.checkWritable(element);
		final CompositeElement attrNode = (CompositeElement) element.getNode();
		final ASTNode valueNode = attrNode.findLeafElementAt(range.getStartOffset());
		LOG.assertTrue(valueNode != null, "Leaf not found in " + attrNode + " at offset " + range.getStartOffset() + " in element " + element);
		final PsiElement elementToReplace = valueNode.getPsi();

		String text;
		try
		{
			text = elementToReplace.getText();
			final int offsetInParent = elementToReplace.getStartOffsetInParent();
			String textBeforeRange = text.substring(0, range.getStartOffset() - offsetInParent);
			String textAfterRange = text.substring(range.getEndOffset() - offsetInParent, text.length());
			text = textBeforeRange + newContent + textAfterRange;
		}
		catch(StringIndexOutOfBoundsException e)
		{
			LOG.error("Range: " + range + " in text: '" + element.getText() + "'", e);
			throw e;
		}
		final CharTable charTableByTree = SharedImplUtil.findCharTableByTree(attrNode);
		final LeafElement newValueElement = Factory.createSingleLeafElement(tokenType, text, charTableByTree, element.getManager());

		attrNode.replaceChildInternal(valueNode, newValueElement);
		return element;
	}

	public TextRange getRangeInElement(final XmlAttributeValue xmlAttributeValue)
	{
		final PsiElement child = xmlAttributeValue.getFirstChild();
		if(child == null)
		{
			return TextRange.EMPTY_RANGE;
		}
		final ASTNode node = child.getNode();
		assert node != null;
		final int textLength = xmlAttributeValue.getTextLength();
		if(node.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER)
		{
			return new TextRange(1, textLength <= 1 ? 1 : textLength - 1);
		}
		else
		{
			return new TextRange(0, textLength);
		}
	}

	@Nonnull
	@Override
	public Class<XmlAttributeValue> getElementClass()
	{
		return XmlAttributeValue.class;
	}
}
