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
package consulo.xml.ide.highlighter;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeWithPredefinedCharset;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;

import jakarta.annotation.Nonnull;
import java.nio.charset.Charset;

public class XmlFileType extends XmlLikeFileType implements DomSupportEnabled, FileTypeWithPredefinedCharset
{
	public static final XmlFileType INSTANCE = new XmlFileType();
	public static final String DEFAULT_EXTENSION = "xml";
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
	public LocalizeValue getDescription()
	{
		return XmlLocalize.filetypeDescriptionXml();
	}

	@Override
	@Nonnull
	public String getDefaultExtension()
	{
		return DEFAULT_EXTENSION;
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return PlatformIconGroup.filetypesXml();
	}

	@Nonnull
	@Override
	public Pair<Charset, String> getPredefinedCharset(@Nonnull VirtualFile virtualFile)
	{
		return Pair.create(virtualFile.getCharset(), "XML file");
	}
}
