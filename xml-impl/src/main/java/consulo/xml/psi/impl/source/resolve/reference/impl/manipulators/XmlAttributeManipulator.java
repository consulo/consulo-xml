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
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlChildRole;

import jakarta.annotation.Nonnull;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class XmlAttributeManipulator extends AbstractElementManipulator<XmlAttribute>
{
	private final static Logger LOG = Logger.getInstance(XmlAttributeManipulator.class);

	public XmlAttribute handleContentChange(XmlAttribute attribute, TextRange range, String newContent) throws IncorrectOperationException
	{
		String attr = attribute.getText();
		ASTNode astNode = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(attribute.getNode());
		assert astNode != null;
		PsiElement namePsi = astNode.getPsi();
		int startOffsetInParent = namePsi.getStartOffsetInParent();
		TextRange nameRange = new TextRange(startOffsetInParent, startOffsetInParent + namePsi.getTextLength());

		if(nameRange.contains(range))
		{
			String content = attr.substring(0, range.getStartOffset()) + newContent + attr.substring(range.getEndOffset(), nameRange.getEndOffset());

			attribute.setName(content);
		}
		else
		{
			final XmlAttributeValue value = attribute.getValueElement();

			if(value == null)
			{
				assert range.getStartOffset() == 0 && range.getEndOffset() == 0;
				attribute.setValue(newContent);
				return attribute;
			}
			final StringBuilder replacement = new StringBuilder(value.getText());
			int offset = value.getTextRange().getStartOffset() - attribute.getTextRange().getStartOffset();

			replacement.replace(
					range.getStartOffset() - offset,
					range.getEndOffset() - offset,
					newContent
			);
			attribute.setValue(replacement.toString());
		}
		return attribute;
	}

	public TextRange getRangeInElement(final XmlAttribute attribute)
	{
		final XmlAttributeValue value = attribute.getValueElement();
		if(value == null)
		{
			return TextRange.from(0, 0);
		}
		TextRange range = attribute.getValueTextRange();
		if(range == null)
		{
			LOG.error("Null range in " + attribute + " '" + attribute.getText() + "'");
		}
		return range.shiftRight(value.getStartOffsetInParent());
	}

	@Nonnull
	@Override
	public Class<XmlAttribute> getElementClass()
	{
		return XmlAttribute.class;
	}
}