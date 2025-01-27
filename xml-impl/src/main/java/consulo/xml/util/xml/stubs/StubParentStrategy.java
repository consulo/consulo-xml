/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.impl.DomInvocationHandler;
import consulo.xml.util.xml.impl.DomManagerImpl;
import consulo.xml.util.xml.impl.DomParentStrategy;
import consulo.xml.util.xml.impl.PhysicalDomParentStrategy;
import consulo.xml.util.xml.impl.VirtualDomParentStrategy;

/**
 * @author Dmitry Avdeev
 *         Date: 8/9/12
 */
public class StubParentStrategy implements DomParentStrategy
{

	public static StubParentStrategy createAttributeStrategy(@Nullable AttributeStub stub, @Nonnull final DomStub parent)
	{
		if(stub == null)
		{
			return new Empty(parent);
		}
		else
		{
			return new StubParentStrategy(stub)
			{
				@Override
				public XmlElement getXmlElement()
				{
					DomInvocationHandler parentHandler = getParentHandler();
					assert parentHandler != null;
					XmlTag tag = parentHandler.getXmlTag();
					if(tag == null)
					{
						throw new AssertionError("can't find tag for " + parentHandler);
					}
					return tag.getAttribute(myStub.getName());
				}
			};
		}
	}

	protected final DomStub myStub;

	public StubParentStrategy(@Nonnull DomStub stub)
	{
		myStub = stub;
	}

	@Override
	public DomInvocationHandler getParentHandler()
	{
		DomStub parentStub = myStub.getParentStub();
		return parentStub == null ? null : parentStub.getHandler();
	}

	@Override
	public XmlElement getXmlElement()
	{
		DomStub parentStub = myStub.getParentStub();
		if(parentStub == null)
		{
			return null;
		}
		List<DomStub> children = parentStub.getChildrenStubs();
		if(children.isEmpty())
		{
			return null;
		}
		XmlTag parentTag = parentStub.getHandler().getXmlTag();
		if(parentTag == null)
		{
			return null;
		}

		// for custom elements, namespace information is lost
		// todo: propagate ns info through DomChildDescriptions
		XmlTag[] tags = parentTag.getSubTags();

		int i = 0;
		String nameToFind = myStub.getName();
		for(XmlTag xmlTag : tags)
		{
			if(nameToFind.equals(xmlTag.getName()) && myStub.getIndex() == i++)
			{
				return xmlTag;
			}
		}
		return null;
	}

	@Nonnull
	@Override
	public DomParentStrategy refreshStrategy(DomInvocationHandler handler)
	{
		return this;
	}

	@Nonnull
	@Override
	public DomParentStrategy setXmlElement(@Nonnull XmlElement element)
	{
		return new PhysicalDomParentStrategy(element, DomManagerImpl.getDomManager(element.getProject()));
	}

	@Nonnull
	@Override
	public DomParentStrategy clearXmlElement()
	{
		final DomInvocationHandler parent = getParentHandler();
		assert parent != null : "write operations should be performed on the DOM having a parent, your DOM may be not very fresh";
		return new VirtualDomParentStrategy(parent);
	}

	@Override
	public String checkValidity()
	{
		return null;
	}

	@Override
	public XmlFile getContainingFile(DomInvocationHandler handler)
	{
		return getParentHandler().getFile();
	}

	@Override
	public boolean isPhysical()
	{
		return true;
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object obj)
	{
		return PhysicalDomParentStrategy.strategyEquals(this, obj);
	}

	public static class Empty extends StubParentStrategy
	{
		private final DomStub myParent;

		public Empty(DomStub parent)
		{
			super(parent);
			myParent = parent;
		}

		@Override
		public DomInvocationHandler getParentHandler()
		{
			return myParent.getHandler();
		}

		@Override
		public XmlElement getXmlElement()
		{
			return null;
		}

		@Override
		public boolean isPhysical()
		{
			return false;
		}
	}
}
