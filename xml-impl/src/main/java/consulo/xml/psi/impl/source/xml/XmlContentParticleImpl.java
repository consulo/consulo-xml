/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.language.psi.PsiElement;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.DtdResolveUtil;
import consulo.language.ast.IElementType;
import consulo.xml.psi.xml.XmlContentParticle;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import com.intellij.xml.XmlElementDescriptor;
import consulo.language.psi.PsiWhiteSpace;

/**
 * @author Dmitry Avdeev
 */
public class XmlContentParticleImpl implements XmlContentParticle, XmlTokenType
{

	private final XmlToken myToken;

	public XmlContentParticleImpl(XmlToken token)
	{
		myToken = token;
	}

	@Override
	public Type getType()
	{
		return Type.ELEMENT;
	}

	@Override
	public Quantifier getQuantifier()
	{
		return getQuantifierImpl(myToken);
	}

	public static Quantifier getQuantifierImpl(PsiElement element)
	{
		PsiElement nextSibling = element.getNextSibling();
		while(nextSibling instanceof PsiWhiteSpace)
		{
			nextSibling = nextSibling.getNextSibling();
		}

		if(nextSibling instanceof XmlToken)
		{
			IElementType tokenType = ((XmlToken) nextSibling).getTokenType();
			if(tokenType == XML_PLUS)
			{
				return Quantifier.ONE_OR_MORE;
			}
			else if(tokenType == XML_STAR)
			{
				return Quantifier.ZERO_OR_MORE;
			}
			else if(tokenType == XML_QUESTION)
			{
				return Quantifier.OPTIONAL;
			}
		}
		return Quantifier.REQUIRED;
	}

	@Override
	public XmlContentParticle[] getSubParticles()
	{
		return new XmlContentParticle[0];
	}

	@Override
	public XmlElementDescriptor getElementDescriptor()
	{
		return DtdResolveUtil.resolveElementReference(myToken.getText(), myToken);
	}
}
