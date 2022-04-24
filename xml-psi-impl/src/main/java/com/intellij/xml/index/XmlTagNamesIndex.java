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
package com.intellij.xml.index;

import com.intellij.xml.util.XmlUtil;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.project.Project;
import consulo.util.io.Readers;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class XmlTagNamesIndex extends XmlIndex<Void>
{
	public static Collection<VirtualFile> getFilesByTagName(String tagName, final Project project)
	{
		return FileBasedIndex.getInstance().getContainingFiles(NAME, tagName, createFilter(project));
	}

	public static Collection<String> getAllTagNames(Project project)
	{
		return FileBasedIndex.getInstance().getAllKeys(NAME, project);
	}

	static final ID<String, Void> NAME = ID.create("XmlTagNames");

	@Override
	@Nonnull
	public ID<String, Void> getName()
	{
		return NAME;
	}

	@Override
	@Nonnull
	public DataIndexer<String, Void, FileContent> getIndexer()
	{
		return new DataIndexer<String, Void, FileContent>()
		{
			@Override
			@Nonnull
			public Map<String, Void> map(@Nonnull final FileContent inputData)
			{
				CharSequence text = inputData.getContentAsText();
				if(StringUtil.indexOf(text, XmlUtil.XML_SCHEMA_URI) == -1)
				{
					return Collections.emptyMap();
				}
				Collection<String> tags = XsdTagNameBuilder.computeTagNames(Readers.readerFromCharSequence(text));
				Map<String, Void> map = new HashMap<String, Void>(tags.size());
				for(String tag : tags)
				{
					map.put(tag, null);
				}
				return map;
			}
		};
	}

	@Nonnull
	@Override
	public DataExternalizer<Void> getValueExternalizer()
	{
		return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
	}
}
