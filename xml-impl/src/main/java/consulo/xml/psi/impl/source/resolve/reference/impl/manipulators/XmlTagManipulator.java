/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTagValue;
import consulo.xml.psi.xml.XmlText;

import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Maxim.Mossienko
 */
@ExtensionImpl
public class XmlTagManipulator extends AbstractElementManipulator<XmlTag>
{
	private static final Logger LOG = Logger.getInstance(XmlTagManipulator.class);

	@Override
	public XmlTag handleContentChange(@Nonnull XmlTag tag, @Nonnull TextRange range, String newContent) throws IncorrectOperationException
	{
		final StringBuilder replacement = new StringBuilder(tag.getValue().getText());
		final int valueOffset = tag.getValue().getTextRange().getStartOffset() - tag.getTextOffset();

		replacement.replace(range.getStartOffset() - valueOffset, range.getEndOffset() - valueOffset, newContent);
		tag.getValue().setEscapedText(replacement.toString());
		return tag;
	}

	@Override
	@Nonnull
	public TextRange getRangeInElement(@Nonnull final XmlTag tag)
	{
		if(tag.getSubTags().length > 0)
		{
			// Text range in tag with subtags is not supported, return empty range, consider making this function nullable.
			return TextRange.EMPTY_RANGE;
		}

		final XmlTagValue value = tag.getValue();
		final XmlText[] texts = value.getTextElements();
		switch(texts.length)
		{
			case 0:
				TextRange range = value.getTextRange();
				if(range.getStartOffset() <= 0 && range.getEndOffset() <= 0 && tag.getTextOffset() > 0)
				{
					LOG.error("Invalid range for element '%s', text '%s', range '%s', file '%s'".formatted(value.getClass().getName(), value.getText(), range.toString(), Objects.toString(tag.getContainingFile())));
				}
				return range.shiftRight(-tag.getTextOffset());
			case 1:
				return getValueRange(texts[0]);
			default:
				return TextRange.EMPTY_RANGE;
		}
	}

	@Nonnull
	@Override
	public Class<XmlTag> getElementClass()
	{
		return XmlTag.class;
	}

	private static TextRange getValueRange(final XmlText xmlText)
	{
		final int offset = xmlText.getStartOffsetInParent();
		final String value = xmlText.getValue();
		final String trimmed = value.trim();
		final int i = value.indexOf(trimmed);
		final int start = xmlText.displayToPhysical(i) + offset;
		return trimmed.isEmpty() ? new TextRange(start, start) : new TextRange(start, xmlText.displayToPhysical(i + trimmed.length() - 1) + offset + 1);
	}

	public static TextRange[] getValueRanges(@Nonnull final XmlTag tag)
	{
		final XmlTagValue value = tag.getValue();
		final XmlText[] texts = value.getTextElements();
		if(texts.length == 0)
		{
			return new TextRange[]{value.getTextRange().shiftRight(-tag.getTextOffset())};
		}
		else
		{
			final TextRange[] ranges = new TextRange[texts.length];
			for(int i = 0; i < texts.length; i++)
			{
				ranges[i] = getValueRange(texts[i]);
			}
			return ranges;
		}
	}
}
