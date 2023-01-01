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
package consulo.xml.util.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.util.xml.impl.DomApplicationComponent;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.Readers;
import consulo.util.lang.StringUtil;
import consulo.util.xml.fastReader.NanoXmlUtil;
import consulo.util.xml.fastReader.XmlFileHeader;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author peter
 */
@ExtensionImpl
public class DomFileIndex extends ScalarIndexExtension<String>
{
	public static final ID<String, Void> NAME = ID.create("DomFileIndex");
	private final DataIndexer<String, Void, FileContent> myDataIndexer;

	public DomFileIndex()
	{
		myDataIndexer = new DataIndexer<String, Void, FileContent>()
		{
			@Override
			@Nonnull
			public Map<String, Void> map(final FileContent inputData)
			{
				final Set<String> namespaces = new HashSet<String>();
				final XmlFileHeader header = NanoXmlUtil.parseHeader(Readers.readerFromCharSequence(inputData.getContentAsText()));
				ContainerUtil.addIfNotNull(namespaces, header.getPublicId());
				ContainerUtil.addIfNotNull(namespaces, header.getSystemId());
				ContainerUtil.addIfNotNull(namespaces, header.getRootTagNamespace());
				final String tagName = header.getRootTagLocalName();
				if(StringUtil.isNotEmpty(tagName))
				{
					final Map<String, Void> result = new HashMap<String, Void>();
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
	@Nonnull
	public ID<String, Void> getName()
	{
		return NAME;
	}

	@Override
	@Nonnull
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
