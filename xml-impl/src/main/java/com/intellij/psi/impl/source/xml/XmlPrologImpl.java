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
package com.intellij.psi.impl.source.xml;

import javax.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.logging.Logger;
import consulo.language.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import consulo.ide.impl.psi.tree.ChildRoleBase;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlDoctype;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlProlog;

/**
 * @author Mike
 */
public class XmlPrologImpl extends XmlElementImpl implements XmlProlog, XmlElementType
{
	private static final Logger LOG = Logger.getInstance("#XmlPrologImpl");

	public XmlPrologImpl()
	{
		super(XML_PROLOG);
	}

	@Override
	public void accept(@Nonnull PsiElementVisitor visitor)
	{
		if(visitor instanceof XmlElementVisitor)
		{
			((XmlElementVisitor) visitor).visitXmlProlog(this);
		}
		else
		{
			visitor.visitElement(this);
		}
	}

	@Override
	public int getChildRole(ASTNode child)
	{
		LOG.assertTrue(child.getTreeParent() == this);
		if(child.getElementType() == XML_DOCTYPE)
		{
			return XmlChildRole.XML_DOCTYPE;
		}
		else
		{
			return ChildRoleBase.NONE;
		}
	}

	@Override
	public XmlDoctype getDoctype()
	{
		return (XmlDoctype) findChildByRoleAsPsiElement(XmlChildRole.XML_DOCTYPE);
	}
}