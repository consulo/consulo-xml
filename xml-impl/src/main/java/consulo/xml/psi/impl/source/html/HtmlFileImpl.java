/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.xml.psi.impl.source.html;

import consulo.language.file.FileViewProvider;
import consulo.language.impl.ast.CompositeElement;
import consulo.xml.psi.impl.source.xml.XmlFileImpl;
import consulo.language.ast.IFileElementType;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlElementType;
import consulo.language.ast.ASTNode;

/**
 * @author maxim
 */
public class HtmlFileImpl extends XmlFileImpl
{
	public HtmlFileImpl(FileViewProvider provider)
	{
		this(provider, XmlElementType.HTML_FILE);
	}

	public HtmlFileImpl(FileViewProvider provider, IFileElementType type)
	{
		super(provider, type);
	}

	public String toString()
	{

		return "HtmlFile:" + getName();
	}

	@Override
	public XmlDocument getDocument()
	{
		CompositeElement treeElement = calcTreeElement();

		ASTNode node = treeElement.findChildByType(XmlElementType.HTML_DOCUMENT);
		return node != null ? (XmlDocument) node.getPsi() : null;
	}
}
