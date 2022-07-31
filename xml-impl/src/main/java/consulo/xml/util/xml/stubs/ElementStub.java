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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.psi.stub.ObjectStubSerializer;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import consulo.index.io.StringRef;

/**
 * @author Dmitry Avdeev
 *         Date: 8/2/12
 */
public class ElementStub extends DomStub
{

	private final List<DomStub> myChildren = new SmartList<DomStub>();
	private final int myIndex;
	private final boolean myCustom;

	@Nullable
	private final StringRef myElementClass;

	public ElementStub(@Nullable ElementStub parent, @Nonnull StringRef name, @Nullable StringRef namespace, int index, boolean custom,
										 @Nullable StringRef elementClass)
	{
		super(parent, name, namespace);
		myIndex = index;
		myCustom = custom;
		myElementClass = elementClass;
	}

	void addChild(DomStub child)
	{
		myChildren.add(child);
	}

	@Override
	public List<DomStub> getChildrenStubs()
	{
		return myChildren;
	}

	@Override
	public ObjectStubSerializer getStubType()
	{
		return ElementStubSerializer.INSTANCE;
	}

	@Override
	public String toString()
	{
		String key = getNamespaceKey();
		return StringUtil.isEmpty(key) ? getName() : key + ":" + getName();
	}

	public boolean isCustom()
	{
		return myCustom;
	}

	@Override
	public int getIndex()
	{
		return myIndex;
	}

	@Nullable
	String getElementClass()
	{
		return myElementClass == null ? null : myElementClass.getString();
	}
}