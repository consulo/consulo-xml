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

import com.intellij.xml.XmlElementDescriptor;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.psi.xml.XmlContentParticle;
import consulo.xml.psi.xml.XmlElementContentGroup;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlToken;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
public class XmlElementContentGroupImpl extends XmlElementImpl implements XmlElementContentGroup,
		XmlElementType
{

	private Supplier<XmlContentParticle[]> myParticles = LazyValue.notNull(() -> {
		return ContainerUtil.map(getChildren(TokenSet.create(XML_ELEMENT_CONTENT_GROUP, XML_NAME)), new Function<ASTNode, XmlContentParticle>()
		{
			@Override
			public XmlContentParticle apply(ASTNode astNode)
			{
				PsiElement element = astNode.getPsi();
				assert element != null;
				return element instanceof XmlToken ? new XmlContentParticleImpl((XmlToken) element) : (XmlContentParticle) element;
			}
		}, new XmlContentParticle[0]);
	});

	public XmlElementContentGroupImpl()
	{
		super(XML_ELEMENT_CONTENT_GROUP);
	}

	@Override
	public Type getType()
	{
		return findElementByTokenType(XML_BAR) == null ? Type.SEQUENCE : Type.CHOICE;
	}

	@Override
	public Quantifier getQuantifier()
	{
		return XmlContentParticleImpl.getQuantifierImpl(this);
	}

	@Override
	public XmlContentParticle[] getSubParticles()
	{
		return myParticles.get();
	}

	@Override
	public XmlElementDescriptor getElementDescriptor()
	{
		return null;
	}
}
