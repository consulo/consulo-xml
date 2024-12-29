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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.impl.source.parsing.xml.DtdParsing;
import consulo.xml.psi.tree.xml.IXmlLeafElementType;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlElementContentSpec;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlEntityDecl;
import consulo.xml.psi.xml.XmlEntityRef;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlToken;
import com.intellij.xml.util.XmlUtil;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiNamedElement;
import consulo.navigation.Navigatable;
import consulo.language.util.IncorrectOperationException;

/**
 * @author mike
 */
public class XmlEntityDeclImpl extends XmlElementImpl implements XmlEntityDecl, XmlElementType
{
	public XmlEntityDeclImpl()
	{
		super(XML_ENTITY_DECL);
	}

	@Override
	public PsiElement getNameElement()
	{
		for(ASTNode e = getFirstChildNode(); e != null; e = e.getTreeNext())
		{
			if(e instanceof XmlTokenImpl)
			{
				XmlTokenImpl xmlToken = (XmlTokenImpl) e;

				if(xmlToken.getTokenType() == XML_NAME)
				{
					return xmlToken;
				}
			}
		}

		return null;
	}

	@Override
	public XmlAttributeValue getValueElement()
	{
		if(isInternalReference())
		{
			for(ASTNode e = getFirstChildNode(); e != null; e = e.getTreeNext())
			{
				if(e.getElementType() == XML_ATTRIBUTE_VALUE)
				{
					return (XmlAttributeValue) SourceTreeToPsiMap.treeElementToPsi(e);
				}
			}
		}
		else
		{
			for(ASTNode e = getLastChildNode(); e != null; e = e.getTreePrev())
			{
				if(e.getElementType() == XML_ATTRIBUTE_VALUE)
				{
					return (XmlAttributeValue) SourceTreeToPsiMap.treeElementToPsi(e);
				}
			}
		}

		return null;
	}

	@Override
	public String getName()
	{
		PsiElement nameElement = getNameElement();
		return nameElement != null ? nameElement.getText() : "";
	}

	@Override
	public PsiElement setName(@Nonnull String name) throws IncorrectOperationException
	{
		final PsiElement nameElement = getNameElement();

		if(nameElement != null)
		{
			return ElementManipulators.getManipulator(nameElement).handleContentChange(nameElement, new TextRange(0, nameElement.getTextLength()), name);
		}
		return null;
	}

	@Override
	public PsiElement parse(PsiFile baseFile, EntityContextType contextType, final XmlEntityRef originalElement)
	{
		PsiElement dep = DEPENDING_ELEMENT.get(getParent());
		PsiElement dependsOnElement = getValueElement(dep instanceof PsiFile ? (PsiFile) dep : baseFile);
		String value = null;
		if(dependsOnElement instanceof XmlAttributeValue)
		{
			XmlAttributeValue attributeValue = (XmlAttributeValue) dependsOnElement;
			value = attributeValue.getValue();
		}
		else if(dependsOnElement instanceof PsiFile)
		{
			PsiFile file = (PsiFile) dependsOnElement;
			value = file.getText();
		}

		if(value == null)
		{
			return null;
		}

		DtdParsing dtdParsing = new DtdParsing(value, XML_ELEMENT_DECL, contextType, baseFile);
		PsiElement generated = dtdParsing.parse().getPsi().getFirstChild();
		if(contextType == EntityContextType.ELEMENT_CONTENT_SPEC && generated instanceof XmlElementContentSpec)
		{
			generated = generated.getFirstChild();
		}
		setDependsOnElement(generated, dependsOnElement);
		return setOriginalElement(generated, originalElement);
	}

	private PsiElement setDependsOnElement(PsiElement generated, PsiElement dependsOnElement)
	{
		PsiElement e = generated;
		while(e != null)
		{
			e.putUserData(DEPENDING_ELEMENT, dependsOnElement);
			e = e.getNextSibling();
		}
		return generated;
	}

	private PsiElement setOriginalElement(PsiElement element, PsiElement valueElement)
	{
		PsiElement e = element;
		while(e != null)
		{
			e.putUserData(INCLUDING_ELEMENT, (XmlElement) valueElement);
			e = e.getNextSibling();
		}
		return element;
	}

	@Nullable
	private PsiElement getValueElement(PsiFile baseFile)
	{
		final XmlAttributeValue attributeValue = getValueElement();
		if(isInternalReference())
		{
			return attributeValue;
		}

		if(attributeValue != null)
		{
			final String value = attributeValue.getValue();
			if(value != null)
			{
				XmlFile xmlFile = XmlUtil.findNamespaceByLocation(baseFile, value);
				if(xmlFile != null)
				{
					return xmlFile;
				}

				final int i = XmlUtil.getPrefixLength(value);
				if(i > 0)
				{
					return XmlUtil.findNamespaceByLocation(baseFile, value.substring(i));
				}
			}
		}

		return null;
	}

	@Override
	public boolean isInternalReference()
	{
		for(ASTNode e = getFirstChildNode(); e != null; e = e.getTreeNext())
		{
			if(e.getElementType() instanceof IXmlLeafElementType)
			{
				XmlToken token = (XmlToken) SourceTreeToPsiMap.treeElementToPsi(e);
				if(token.getTokenType() == XML_DOCTYPE_PUBLIC || token.getTokenType() == XML_DOCTYPE_SYSTEM)
				{
					return false;
				}
			}
		}

		return true;
	}

	@Override
	@Nonnull
	public PsiElement getNavigationElement()
	{
		return getNameElement();
	}

	@Override
	public int getTextOffset()
	{
		final PsiElement name = getNameElement();
		return name != null ? name.getTextOffset() : super.getTextOffset();
	}

	@Override
	public boolean canNavigate()
	{
		if(isPhysical())
		{
			return super.canNavigate();
		}
		final PsiNamedElement psiNamedElement = XmlUtil.findRealNamedElement(this);
		return psiNamedElement != null;
	}

	@Override
	public void navigate(final boolean requestFocus)
	{
		if(!isPhysical())
		{
			((Navigatable) XmlUtil.findRealNamedElement(this)).navigate(requestFocus);
			return;
		}
		super.navigate(requestFocus);
	}
}
