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
package com.intellij.psi;

import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.ide.highlighter.XHtmlFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.util.XmlTagUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@Singleton
public class XmlElementFactoryImpl extends XmlElementFactory
{
	private final Project myProject;

	@Inject
	public XmlElementFactoryImpl(Project project)
	{
		myProject = project;
	}

	@Override
	@Nonnull
	public XmlTag createTagFromText(@Nonnull @NonNls CharSequence text, @Nonnull Language language) throws IncorrectOperationException
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
	@Nonnull
	public XmlTag createTagFromText(@Nonnull CharSequence text) throws IncorrectOperationException
	{
		return createTagFromText(text, XMLLanguage.INSTANCE);
	}

	@Override
	@Nonnull
	public XmlAttribute createXmlAttribute(@Nonnull String name, @Nonnull String value) throws IncorrectOperationException
	{
		return createAttribute(name, value, XmlFileType.INSTANCE);
	}

	@Nonnull
	@Override
	public XmlAttribute createAttribute(@Nonnull @NonNls String name, @Nonnull String value, @Nullable PsiElement context) throws IncorrectOperationException
	{
		return createAttribute(name, value, PsiTreeUtil.getParentOfType(context, XmlTag.class, false) instanceof HtmlTag ? HtmlFileType.INSTANCE : XmlFileType.INSTANCE);
	}

	@Nonnull
	private XmlAttribute createAttribute(@Nonnull String name, @Nonnull String value, @Nonnull FileType fileType)
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
	@Nonnull
	public XmlText createDisplayText(@Nonnull String s) throws IncorrectOperationException
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
	@Nonnull
	public XmlTag createXHTMLTagFromText(@Nonnull String text) throws IncorrectOperationException
	{
		final XmlDocument document = createXmlDocument(text, "dummy.xhtml", XHtmlFileType.INSTANCE);
		final XmlTag tag = document.getRootTag();
		assert tag != null;
		return tag;
	}

	@Override
	@Nonnull
	public XmlTag createHTMLTagFromText(@Nonnull String text) throws IncorrectOperationException
	{
		final XmlDocument document = createXmlDocument(text, "dummy.html", HtmlFileType.INSTANCE);
		final XmlTag tag = document.getRootTag();
		assert tag != null;
		return tag;
	}

	private XmlDocument createXmlDocument(@NonNls final CharSequence text, @NonNls final String fileName, FileType fileType)
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
