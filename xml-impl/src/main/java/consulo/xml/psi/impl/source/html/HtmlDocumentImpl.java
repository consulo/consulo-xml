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
package consulo.xml.psi.impl.source.html;

import consulo.xml.psi.impl.source.xml.XmlDocumentImpl;
import consulo.language.ast.IElementType;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTag;

/**
 * @author Maxim.Mossienko
 */
public class HtmlDocumentImpl extends XmlDocumentImpl
{
	public HtmlDocumentImpl()
	{
		super(XmlElementType.HTML_DOCUMENT);
	}

	public HtmlDocumentImpl(IElementType type)
	{
		super(type);
	}

	@Override
	public XmlTag getRootTag()
	{
		return (XmlTag) findElementByTokenType(XmlElementType.HTML_TAG);
	}
}
