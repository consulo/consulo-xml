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
package com.intellij.util.xml;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.xml.impl.DomApplicationComponent;

/**
 * @author peter
 */
public class DomFileIndex extends ScalarIndexExtension<String>
{
	public static final ID<String, Void> NAME = ID.create("DomFileIndex");
	private final DataIndexer<String, Void, FileContent> myDataIndexer;

	public DomFileIndex()
	{
		myDataIndexer = new DataIndexer<String, Void, FileContent>()
		{
			@Override
			@NotNull
			public Map<String, Void> map(final FileContent inputData)
			{
				final Set<String> namespaces = new THashSet<String>();
				final XmlFileHeader header = NanoXmlUtil.parseHeader(CharArrayUtil.readerFromCharSequence(inputData.getContentAsText()));
				ContainerUtil.addIfNotNull(namespaces, header.getPublicId());
				ContainerUtil.addIfNotNull(namespaces, header.getSystemId());
				ContainerUtil.addIfNotNull(namespaces, header.getRootTagNamespace());
				final String tagName = header.getRootTagLocalName();
				if(StringUtil.isNotEmpty(tagName))
				{
					final THashMap<String, Void> result = new THashMap<String, Void>();
					final DomApplicationComponent component = DomApplicationComponent.getInstance();
					for(final DomFileDescription description : component.getFileDescriptions(tagName))
					{
						final String[] strings = description.getAllPossibleRootTagNamespaces();
						if(strings.length == 0 || ContainerUtil.intersects(Arrays.asList(strings), namespaces))
						{
							result.put(description.getRootElementClass().getName(), null);
						}
					}
					for(final DomFileDescription description : component.getAcceptingOtherRootTagNameDescriptions())
					{
						final String[] strings = description.getAllPossibleRootTagNamespaces();
						if(strings.length == 0 || ContainerUtil.intersects(Arrays.asList(strings), namespaces))
						{
							result.put(description.getRootElementClass().getName(), null);
						}
					}
					return result;
				}
				return Collections.emptyMap();
			}
		};
	}

	@Override
	@NotNull
	public ID<String, Void> getName()
	{
		return NAME;
	}

	@Override
	@NotNull
	public DataIndexer<String, Void, FileContent> getIndexer()
	{
		return myDataIndexer;
	}

	@Override
	public KeyDescriptor<String> getKeyDescriptor()
	{
		return new EnumeratorStringDescriptor();
	}

	@Override
	public FileBasedIndex.InputFilter getInputFilter()
	{
		return new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE);
	}

	@Override
	public boolean dependsOnFileContent()
	{
		return true;
	}

	@Override
	public int getVersion()
	{
		return DomApplicationComponent.getInstance().getCumulativeVersion();
	}
}