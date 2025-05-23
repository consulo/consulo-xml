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
package consulo.xml.psi.impl.source.xml;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.impl.source.xml.behavior.DefaultXmlPsiPolicy;
import consulo.xml.psi.xml.*;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class XmlTagValueImpl implements XmlTagValue
{
	private static final Logger LOG = Logger.getInstance("#XmlTagValueImpl");

	private final XmlTag myTag;
	private final XmlTagChild[] myElements;
	private volatile XmlText[] myTextElements = null;
	private volatile String myText = null;
	private volatile String myTrimmedText = null;

	public XmlTagValueImpl(@Nonnull XmlTagChild[] bodyElements, @Nonnull XmlTag tag)
	{
		myTag = tag;
		myElements = bodyElements;
	}

	@Override
	@Nonnull
	public XmlTagChild[] getChildren()
	{
		return myElements;
	}

	@Override
	@Nonnull
	public XmlText[] getTextElements()
	{
		XmlText[] textElements = myTextElements;
		if(textElements != null)
		{
			return textElements;
		}
		final List<XmlText> textElementsList = new ArrayList<XmlText>();
		for(final XmlTagChild element : myElements)
		{
			if(element instanceof XmlText)
			{
				textElementsList.add((XmlText) element);
			}
		}
		return myTextElements = textElementsList.isEmpty() ? XmlText.EMPTY_ARRAY : textElementsList.toArray(new XmlText[textElementsList.size()]);
	}

	@Override
	@Nonnull
	public String getText()
	{
		String text = myText;
		if(text != null)
		{
			return text;
		}
		final StringBuilder consolidatedText = new StringBuilder();
		for(final XmlTagChild element : myElements)
		{
			consolidatedText.append(element.getText());
		}
		return myText = consolidatedText.toString();
	}

	@Override
	@Nonnull
	public TextRange getTextRange()
	{
		if(myElements.length == 0)
		{
			final ASTNode child = XmlChildRole.START_TAG_END_FINDER.findChild((ASTNode) myTag);
			if(child != null)
			{
				return new TextRange(child.getStartOffset() + 1, child.getStartOffset() + 1);
			}
			return new TextRange(myTag.getTextRange().getEndOffset(), myTag.getTextRange().getEndOffset());
		}
		return new TextRange(myElements[0].getTextRange().getStartOffset(), myElements[myElements.length - 1].getTextRange().getEndOffset());
	}

	@Override
	@Nonnull
	public String getTrimmedText()
	{
		String trimmedText = myTrimmedText;
		if(trimmedText != null)
		{
			return trimmedText;
		}

		final StringBuilder consolidatedText = new StringBuilder();
		final XmlText[] textElements = getTextElements();
		for(final XmlText textElement : textElements)
		{
			consolidatedText.append(textElement.getValue());
		}
		return myTrimmedText = consolidatedText.toString().trim();
	}

	@Override
	public void setText(String value)
	{
		setText(value, false);
	}

	@Override
	public void setEscapedText(String value)
	{
		setText(value, true);
	}

	private void setText(String value, boolean defaultPolicy)
	{
		try
		{
			XmlText text = null;
			if(value != null)
			{
				final XmlText[] texts = getTextElements();
				if(texts.length == 0)
				{
					text = (XmlText) myTag.add(XmlElementFactory.getInstance(myTag.getProject()).createDisplayText("x"));
				}
				else
				{
					text = texts[0];
				}
				if(StringUtil.isEmpty(value))
				{
					text.delete();
				}
				else
				{
					if(defaultPolicy && text instanceof XmlTextImpl)
					{
						((XmlTextImpl) text).doSetValue(value, new DefaultXmlPsiPolicy()
						{
							@Nonnull
							@Override
							public Language getLanguage()
							{
								return XMLLanguage.INSTANCE;
							}
						});
					}
					else
					{
						text.setValue(value);
					}
				}
			}

			if(myElements.length > 0)
			{
				for(final XmlTagChild child : myElements)
				{
					if(child != text)
					{
						child.delete();
					}
				}
			}
		}
		catch(IncorrectOperationException e)
		{
			LOG.error(e);
		}
	}

	@Override
	public boolean hasCDATA()
	{
		for(XmlText xmlText : myTextElements)
		{
			PsiElement[] children = xmlText.getChildren();
			for(PsiElement child : children)
			{
				if(child.getNode().getElementType() == XmlElementType.XML_CDATA)
				{
					return true;
				}
			}
		}
		return false;
	}

	public static XmlTagValue createXmlTagValue(XmlTag tag)
	{
		final List<XmlTagChild> bodyElements = new ArrayList<XmlTagChild>();

		tag.processElements(new PsiElementProcessor()
		{
			boolean insideBody = false;

			@Override
			public boolean execute(@Nonnull PsiElement element)
			{
				final ASTNode treeElement = element.getNode();
				if(insideBody)
				{
					if(treeElement != null && treeElement.getElementType() == XmlTokenType.XML_END_TAG_START)
					{
						return false;
					}
					if(!(element instanceof XmlTagChild))
					{
						return true;
					}
					bodyElements.add((XmlTagChild) element);
				}
				else if(treeElement != null && treeElement.getElementType() == XmlTokenType.XML_TAG_END)
				{
					insideBody = true;
				}
				return true;
			}
		}, tag);

		XmlTagChild[] tagChildren = bodyElements.toArray(new XmlTagChild[bodyElements.size()]);
		return new XmlTagValueImpl(tagChildren, tag);
	}
}
