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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import consulo.xml.psi.XmlRecursiveElementVisitor;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.TypeOrElementOrAttributeReference;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.util.IncorrectOperationException;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.intention.PsiElementBaseIntentionAction;
import consulo.language.editor.WriteCommandAction;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class AddSchemaPrefixIntention extends PsiElementBaseIntentionAction
{
	public static final String NAME = "Insert Namespace Prefix";

	public AddSchemaPrefixIntention()
	{
		setText(NAME);
	}

	@Nonnull
	@Override
	public String getFamilyName()
	{
		return NAME;
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}

	@Override
	public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException
	{
		final XmlAttribute xmlns = getXmlnsDeclaration(element);
		if(xmlns == null)
		{
			return;
		}
		final String namespace = xmlns.getValue();
		final XmlTag tag = xmlns.getParent();

		if(tag != null)
		{
			final Set<String> ns = tag.getLocalNamespaceDeclarations().keySet();
			final String nsPrefix = Messages.showInputDialog(project, "Namespace Prefix:", NAME, Messages.getInformationIcon(), "", new InputValidator()
			{
				@RequiredUIAccess
				@Override
				public boolean checkInput(String inputString)
				{
					return !ns.contains(inputString);
				}

				@RequiredUIAccess
				@Override
				public boolean canClose(String inputString)
				{
					return checkInput(inputString);
				}
			});
			if(nsPrefix == null)
			{
				return;
			}
			final List<XmlTag> tags = new ArrayList<XmlTag>();
			final List<XmlAttributeValue> values = new ArrayList<XmlAttributeValue>();
			new WriteCommandAction(project, NAME, tag.getContainingFile())
			{
				@Override
				protected void run(Result result) throws Throwable
				{
					tag.accept(new XmlRecursiveElementVisitor()
					{
						@Override
						public void visitXmlTag(XmlTag tag)
						{
							if(tag.getNamespace().equals(namespace) && tag.getNamespacePrefix().length() == 0)
							{
								tags.add(tag);
							}
							super.visitXmlTag(tag);
						}

						@Override
						public void visitXmlAttributeValue(XmlAttributeValue value)
						{
							PsiReference ref = null;
							boolean skip = false;
							for(PsiReference reference : value.getReferences())
							{
								if(reference instanceof TypeOrElementOrAttributeReference)
								{
									ref = reference;
								}
								else if(reference instanceof SchemaPrefixReference)
								{
									skip = true;
									break;
								}
							}
							if(!skip && ref != null)
							{
								final PsiElement xmlElement = ref.resolve();
								if(xmlElement instanceof XmlElement)
								{
									final XmlTag tag = PsiTreeUtil.getParentOfType(xmlElement, XmlTag.class, false);
									if(tag != null)
									{
										if(tag.getNamespace().equals(namespace))
										{
											if(ref.getRangeInElement().getLength() == value.getValue().length())
											{ //no ns prefix
												values.add(value);
											}
										}
									}
								}
							}
						}
					});
					for(XmlAttributeValue value : values)
					{
						((XmlAttribute) value.getParent()).setValue(nsPrefix + ":" + value.getValue());
					}
					for(XmlTag xmlTag : tags)
					{
						xmlTag.setName(nsPrefix + ":" + xmlTag.getLocalName());
					}
					xmlns.setName("xmlns:" + nsPrefix);
				}
			}.execute();
		}
	}

	@Override
	public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element)
	{
		return getXmlnsDeclaration(element) != null;
	}

	@Nullable
	private static XmlAttribute getXmlnsDeclaration(PsiElement element)
	{
		final PsiElement parent = element.getParent();
		if(parent instanceof XmlTag)
		{
			XmlTag tag = (XmlTag) parent;
			if(tag.getNamespacePrefix().length() == 0)
			{
				while(tag != null)
				{
					final XmlAttribute attr = tag.getAttribute("xmlns");
					if(attr != null)
					{
						return attr;
					}
					tag = tag.getParentTag();
				}
			}
		}
		else if(parent instanceof XmlAttribute && ((XmlAttribute) parent).getName().equals("xmlns"))
		{
			return (XmlAttribute) parent;
		}
		return null;
	}
}
