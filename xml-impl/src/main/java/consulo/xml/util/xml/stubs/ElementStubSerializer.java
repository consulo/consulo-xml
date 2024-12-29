/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.util.xml.stubs;

import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.ObjectStubSerializer;
import consulo.language.psi.stub.StubInputStream;
import consulo.language.psi.stub.StubOutputStream;
import consulo.util.lang.StringUtil;
import consulo.xml.util.xml.stubs.index.DomElementClassIndex;
import consulo.xml.util.xml.stubs.index.DomNamespaceKeyIndex;

import jakarta.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Dmitry Avdeev
 *         Date: 8/3/12
 */
public class ElementStubSerializer implements ObjectStubSerializer<ElementStub, ElementStub>
{
	@Override
	public void serialize(@Nonnull ElementStub stub, @Nonnull StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeName(stub.getNamespaceKey());
		dataStream.writeVarInt(stub.getIndex());
		dataStream.writeBoolean(stub.isCustom());
		dataStream.writeName(stub.getElementClass());
	}

	@Nonnull
	@Override
	public ElementStub deserialize(@Nonnull StubInputStream dataStream, ElementStub parentStub) throws IOException
	{
		return new ElementStub(parentStub, dataStream.readName(), dataStream.readName(), dataStream.readVarInt(), dataStream.readBoolean(),
				dataStream.readName());
	}

	@Override
	public void indexStub(@Nonnull ElementStub stub, @Nonnull IndexSink sink)
	{
		final String namespaceKey = stub.getNamespaceKey();
		if(StringUtil.isNotEmpty(namespaceKey))
		{
			sink.occurrence(DomNamespaceKeyIndex.KEY, namespaceKey);
		}

		final String elementClass = stub.getElementClass();
		if(elementClass != null)
		{
			sink.occurrence(DomElementClassIndex.KEY, elementClass);
		}
	}

	@Nonnull
	@Override
	public String getExternalId()
	{
		return "xml.ElementStubSerializer";
	}

	@Override
	public String toString()
	{
		return "Element";
	}
}
