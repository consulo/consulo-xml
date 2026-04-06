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
package consulo.xml.psi.impl.source.tree.injected;


import consulo.document.util.TextRange;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.psi.impl.source.xml.XmlAttributeValueImpl;
import consulo.xml.language.psi.XmlAttribute;

/**
 * @author cdr
 */
public class XmlAttributeLiteralEscaper extends LiteralTextEscaper<XmlAttributeValueImpl>
{
	private final XmlAttribute myXmlAttribute;

	public XmlAttributeLiteralEscaper(XmlAttributeValueImpl host)
	{
		super(host);
		PsiElement parent = host.getParent();
		myXmlAttribute = parent instanceof XmlAttribute ? (XmlAttribute) parent : XmlElementFactory.getInstance(host.getProject()).createAttribute("a", host.getValue(), parent);
	}

	@Override
	public boolean decode(final TextRange rangeInsideHost, StringBuilder outChars)
	{
		TextRange valueTextRange = myXmlAttribute.getValueTextRange();
		int startInDecoded = myXmlAttribute.physicalToDisplay(rangeInsideHost.getStartOffset() - valueTextRange.getStartOffset());
		int endInDecoded = myXmlAttribute.physicalToDisplay(rangeInsideHost.getEndOffset() - valueTextRange.getStartOffset());
		String displayValue = myXmlAttribute.getDisplayValue();
		//todo investigate IIOB http://www.jetbrains.net/jira/browse/IDEADEV-16796
		startInDecoded = startInDecoded < 0 ? 0 : startInDecoded > displayValue.length() ? displayValue.length() : startInDecoded;
		endInDecoded = endInDecoded < 0 ? 0 : endInDecoded > displayValue.length() ? displayValue.length() : endInDecoded;
		if(startInDecoded > endInDecoded)
		{
			endInDecoded = startInDecoded;
		}
		outChars.append(displayValue, startInDecoded, endInDecoded);
		return true;
	}

	@Override
	public int getOffsetInHost(final int offsetInDecoded, final TextRange rangeInsideHost)
	{
		TextRange valueTextRange = myXmlAttribute.getValueTextRange();
		int displayStart = myXmlAttribute.physicalToDisplay(rangeInsideHost.getStartOffset());

		int dp = myXmlAttribute.displayToPhysical(offsetInDecoded + displayStart - valueTextRange.getStartOffset());
		if(dp == -1)
		{
			return -1;
		}
		return dp + valueTextRange.getStartOffset();
	}

	@Override
	public boolean isOneLine()
	{
		return true;
	}
}
