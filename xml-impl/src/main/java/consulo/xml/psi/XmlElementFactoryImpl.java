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
package consulo.xml.psi;

import consulo.xml.language.psi.util.XmlTagUtil;
import consulo.annotation.component.ServiceImpl;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.ast.ASTFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlDocument;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlElementType;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlText;
import consulo.xml.psi.html.HtmlTag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceImpl
public class XmlElementFactoryImpl extends XmlElementFactory
{
	private final Project myProject;

	@Inject
	public XmlElementFactoryImpl(Project project)
	{
		myProject = project;
	}

	@Override
	public XmlTag createTagFromText(CharSequence text, Language language) throws IncorrectOperationException
	{
		assert language instanceof XMLLanguage : "Tag can be created only for xml language";
		FileType type = language.getAssociatedFileType();
		if(type == null)
		{
			type = XmlFileType.INSTANCE;
		}
		final XmlDocument document = createXmlDocument(text, "dummy." + type.getDefaultExtension(), type);
		final XmlTag tag = document.getRootTag();
		if(tag == null)
		{
			throw new IncorrectOperationException("Incorrect tag text");
		}
		return tag;
	}

	@Override
	public XmlTag createTagFromText(CharSequence text) throws IncorrectOperationException
	{
		return createTagFromText(text, XMLLanguage.INSTANCE);
	}

	@Override
	public XmlAttribute createXmlAttribute(String name, String value) throws IncorrectOperationException
	{
		return createAttribute(name, value, XmlFileType.INSTANCE);
	}

	@Override
	public XmlAttribute createAttribute(String name, String value, @Nullable PsiElement context) throws IncorrectOperationException
	{
		return createAttribute(name, value, PsiTreeUtil.getParentOfType(context, XmlTag.class, false) instanceof HtmlTag ? HtmlFileType.INSTANCE : XmlFileType.INSTANCE);
	}

	private XmlAttribute createAttribute(String name, String value, FileType fileType)
	{
		final char quoteChar;
		if(!value.contains("\""))
		{
			quoteChar = '"';
		}
		else if(!value.contains("'"))
		{
			quoteChar = '\'';
		}
		else
		{
			quoteChar = '"';
			value = StringUtil.replace(value, "\"", "&quot;");
		}
		final XmlDocument document = createXmlDocument("<tag " + name + "=" + quoteChar + value + quoteChar + "/>", "dummy.xml", fileType);
		XmlTag tag = document.getRootTag();
		assert tag != null;
		XmlAttribute[] attributes = tag.getAttributes();
		LOG.assertTrue(attributes.length == 1, document.getText());
		return attributes[0];
	}

	@Override
	public XmlText createDisplayText(String s) throws IncorrectOperationException
	{
		final XmlTag tagFromText = createTagFromText("<a>" + XmlTagUtil.getCDATAQuote(s) + "</a>");
		final XmlText[] textElements = tagFromText.getValue().getTextElements();
		if(textElements.length == 0)
		{
			return (XmlText) ASTFactory.composite(XmlElementType.XML_TEXT);
		}
		return textElements[0];
	}

	@Override
	public XmlTag createXHTMLTagFromText(String text) throws IncorrectOperationException
	{
		final XmlDocument document = createXmlDocument(text, "dummy.xhtml", XHtmlFileType.INSTANCE);
		final XmlTag tag = document.getRootTag();
		assert tag != null;
		return tag;
	}

	@Override
	public XmlTag createHTMLTagFromText(String text) throws IncorrectOperationException
	{
		final XmlDocument document = createXmlDocument(text, "dummy.html", HtmlFileType.INSTANCE);
		final XmlTag tag = document.getRootTag();
		assert tag != null;
		return tag;
	}

	private XmlDocument createXmlDocument(final CharSequence text, final String fileName, FileType fileType)
	{
		PsiFile fileFromText = PsiFileFactory.getInstance(myProject).createFileFromText(fileName, fileType, text);

		XmlFile xmlFile;
		if(fileFromText instanceof XmlFile)
		{
			xmlFile = (XmlFile) fileFromText;
		}
		else
		{
			xmlFile = (XmlFile) fileFromText.getViewProvider().getPsi(((LanguageFileType) fileType).getLanguage());
			assert xmlFile != null;
		}
		XmlDocument document = xmlFile.getDocument();
		assert document != null;
		return document;
	}

	private static final Logger LOG = Logger.getInstance(XmlElementFactoryImpl.class);
}
