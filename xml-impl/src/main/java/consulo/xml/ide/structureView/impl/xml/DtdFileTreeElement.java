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
package consulo.xml.ide.structureView.impl.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;

import consulo.application.AllIcons;
import consulo.language.psi.resolve.PsiElementProcessor;
import org.jetbrains.annotations.NonNls;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.util.lang.Comparing;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.xml.psi.xml.XmlAttlistDecl;
import consulo.xml.psi.xml.XmlAttributeDecl;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlElementDecl;
import consulo.xml.psi.xml.XmlEntityDecl;
import consulo.xml.psi.xml.XmlFile;
import com.intellij.xml.impl.dtd.XmlElementDescriptorImpl;
import com.intellij.xml.util.XmlUtil;
import consulo.ui.image.Image;

public class DtdFileTreeElement extends PsiTreeElementBase<XmlFile>
{
	public DtdFileTreeElement(XmlFile file)
	{
		super(file);
	}

	@Override
	@Nonnull
	public Collection<StructureViewTreeElement> getChildrenBase()
	{
		return collectElements(getElement().getDocument());
	}

	static List<StructureViewTreeElement> collectElements(final XmlElement element)
	{
		final List<StructureViewTreeElement> elements = new ArrayList<>();

		XmlUtil.processXmlElements(element, new PsiElementProcessor()
		{
			@Override
			public boolean execute(@Nonnull final PsiElement element)
			{
				if(element instanceof XmlElementDecl || element instanceof XmlEntityDecl)
				{
					elements.add(new DtdTreeElement((PsiNamedElement) element));
				}
				return true;
			}
		}, false);
		return elements;
	}

	@Override
	public String getPresentableText()
	{
		return getElement().getName();
	}

	private static class DtdTreeElement extends PsiTreeElementBase<PsiNamedElement>
	{
		@NonNls
		private static final String IMPLIED = "implied";
		@NonNls
		private static final String REQUIRED = "required";
		@NonNls
		private static final String FIXED = "fixed";
		@NonNls
		private static final String ID = "id";
		@NonNls
		private static final String IDREF = "idref";
		@NonNls
		private static final String ENUM = "enum";

		public DtdTreeElement(final PsiNamedElement element)
		{
			super(element);
		}

		@Override
		@Nonnull
		public Collection<StructureViewTreeElement> getChildrenBase()
		{
			return Collections.emptyList();
		}

		@Override
		public String getPresentableText()
		{
			final PsiNamedElement namedElement = getElement();
			return namedElement != null ? namedElement.getName() : "";
		}

		@Override
		public Image getIcon()
		{
			return AllIcons.Nodes.Tag;
		}

		@Override
		public String getLocationString()
		{
			final XmlElement owner = (XmlElement) getElement();

			final XmlAttlistDecl[] attLists = owner instanceof XmlElementDecl ? XmlElementDescriptorImpl.getCachedAttributeDeclarations(owner) : XmlAttlistDecl.EMPTY_ARRAY;

			if(attLists.length > 0)
			{
				Map<String, XmlAttributeDecl> attrMap = null;

				final String name = getElement().getName();
				for(XmlAttlistDecl a : attLists)
				{
					final String aname = a.getName();
					if(!Comparing.equal(aname, name))
					{
						continue;
					}
					if(attrMap == null)
					{
						attrMap = new LinkedHashMap<>();
					}

					for(XmlAttributeDecl d : a.getAttributeDecls())
					{
						attrMap.put(d.getName(), d);
					}
				}

				StringBuilder b = null;
				if(attrMap != null)
				{
					for(Map.Entry<String, XmlAttributeDecl> e : attrMap.entrySet())
					{
						if(b == null)
						{
							b = new StringBuilder();
						}
						else
						{
							b.append(", ");
						}
						b.append(e.getKey());
						final XmlAttributeDecl attributeDecl = e.getValue();
						String type = null;

						if(attributeDecl.isIdAttribute())
						{
							type = ID;
						}
						else if(attributeDecl.isIdRefAttribute())
						{
							type = IDREF;
						}
						else if(attributeDecl.isEnumerated())
						{
							type = ENUM;
						}

						if(attributeDecl.isAttributeFixed())
						{
							if(type == null)
							{
								type = FIXED;
							}
							else
							{
								type += " " + FIXED;
							}
						}
						else if(attributeDecl.isAttributeRequired())
						{
							if(type == null)
							{
								type = REQUIRED;
							}
							else
							{
								type += " " + REQUIRED;
							}
						}
						else if(attributeDecl.isAttributeImplied())
						{
							if(type == null)
							{
								type = IMPLIED;
							}
							else
							{
								type += " " + IMPLIED;
							}
						}

						if(type != null)
						{
							b.append(':').append(type);
						}
						final XmlAttributeValue value = attributeDecl.getDefaultValue();

						if(value != null)
						{
							b.append("=").append(value);
						}
					}
				}

				if(b != null)
				{
					return b.toString();
				}
			}
			return super.getLocationString();
		}

		public String toString()
		{
			final String s = getLocationString();
			final String name = getElement().getName();
			if(s == null || s.length() == 0)
			{
				return name;
			}
			return name + " (" + s + ")";
		}
	}


}