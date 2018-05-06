/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.ide.highlighter;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileTypes.FileTypeWithPredefinedCharset;
import consulo.ui.image.Image;

public class XmlFileType extends XmlLikeFileType implements DomSupportEnabled, FileTypeWithPredefinedCharset
{
	public static final XmlFileType INSTANCE = new XmlFileType();
	@NonNls
	public static final String DEFAULT_EXTENSION = "xml";
	@NonNls
	public static final String DOT_DEFAULT_EXTENSION = "." + DEFAULT_EXTENSION;

	private XmlFileType()
	{
		super(XMLLanguage.INSTANCE);
	}

	@Override
	@Nonnull
	public String getId()
	{
		return "XML";
	}

	@Override
	@Nonnull
	public String getDescription()
	{
		return IdeBundle.message("filetype.description.xml");
	}

	@Override
	@Nonnull
	public String getDefaultExtension()
	{
		return DEFAULT_EXTENSION;
	}

	@Override
	public Image getIcon()
	{
		return AllIcons.FileTypes.Xml;
	}

	@Nonnull
	@Override
	public Pair<Charset, String> getPredefinedCharset(@Nonnull VirtualFile virtualFile)
	{
		return Pair.create(virtualFile.getCharset(), "XML file");
	}
}
