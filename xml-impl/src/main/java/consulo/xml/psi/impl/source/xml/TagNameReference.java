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
package consulo.xml.psi.impl.source.xml;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class TagNameReference implements PsiReference
{
	private static final Logger LOG = Logger.getInstance("#TagNameReference");

	protected final boolean myStartTagFlag;
	private final ASTNode myNameElement;

	public TagNameReference(ASTNode nameElement, boolean startTagFlag)
	{
		myStartTagFlag = startTagFlag;
		myNameElement = nameElement;
	}

	@Override
	public PsiElement getElement()
	{
		PsiElement element = myNameElement.getPsi();
		final PsiElement parent = element.getParent();
		return parent instanceof XmlTag ? parent : element;
	}

	@Nullable
	protected XmlTag getTagElement()
	{
		final PsiElement element = getElement();
		if(element == myNameElement.getPsi())
		{
			return null;
		}
		return (XmlTag) element;
	}

	@Override
	public TextRange getRangeInElement()
	{
		final ASTNode nameElement = getNameElement();
		if(nameElement == null)
		{
			return TextRange.EMPTY_RANGE;
		}

		int colon = nameElement.getText().indexOf(':') + 1;
		if(myStartTagFlag)
		{
			final int parentOffset = ((TreeElement) nameElement).getStartOffsetInParent();
			return new TextRange(parentOffset + colon, parentOffset + nameElement.getTextLength());
		}
		else
		{
			final PsiElement element = getElement();
			if(element == myNameElement)
			{
				return new TextRange(colon, myNameElement.getTextLength());
			}

			final int elementLength = element.getTextLength();
			int diffFromEnd = 0;

			for(ASTNode node = element.getNode().getLastChildNode(); node != nameElement && node != null; node = node.getTreePrev())
			{
				diffFromEnd += node.getTextLength();
			}

			final int nameEnd = elementLength - diffFromEnd;
			return new TextRange(nameEnd - nameElement.getTextLength() + colon, nameEnd);
		}
	}

	public ASTNode getNameElement()
	{
		return myNameElement;
	}

	@Override
	public PsiElement resolve()
	{
		final XmlTag tag = getTagElement();
		final XmlElementDescriptor descriptor = tag != null ? tag.getDescriptor() : null;

		if(LOG.isDebugEnabled())
		{
			LOG.debug("Descriptor for tag " + (tag != null ? tag.getName() : "NULL") + " is " + (descriptor != null ? (descriptor.toString() + ": " + descriptor.getClass().getCanonicalName()) :
					"NULL"));
		}

		if(descriptor != null)
		{
			return descriptor instanceof AnyXmlElementDescriptor ? tag : descriptor.getDeclaration();
		}
		return null;
	}

	@Override
	@Nonnull
	public String getCanonicalText()
	{
		return getNameElement().getText();
	}

	@Override
	@Nullable
	public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException
	{
		final XmlTag element = getTagElement();
		if(element == null || !myStartTagFlag)
		{
			return element;
		}

		if(newElementName.indexOf(':') == -1)
		{
			final String namespacePrefix = element.getNamespacePrefix();
			final int index = newElementName.lastIndexOf('.');

			if(index != -1)
			{
				final PsiElement psiElement = resolve();

				if(psiElement instanceof PsiFile || (psiElement != null && psiElement.isEquivalentTo(psiElement.getContainingFile())))
				{
					newElementName = newElementName.substring(0, index);
				}
			}
			newElementName = prependNamespacePrefix(newElementName, namespacePrefix);
		}
		element.setName(newElementName);
		return element;
	}

	private static String prependNamespacePrefix(String newElementName, String namespacePrefix)
	{
		newElementName = (!namespacePrefix.isEmpty() ? namespacePrefix + ":" : namespacePrefix) + newElementName;
		return newElementName;
	}

	@Override
	public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException
	{
		PsiMetaData metaData = null;

		if(element instanceof PsiMetaOwner)
		{
			final PsiMetaOwner owner = (PsiMetaOwner) element;
			metaData = owner.getMetaData();

			if(metaData instanceof XmlElementDescriptor)
			{
				return getTagElement().setName(metaData.getName(getElement())); // TODO: need to evaluate new ns prefix
			}
		}
		else if(element instanceof PsiFile)
		{
			final XmlTag tagElement = getTagElement();
			if(tagElement == null || !myStartTagFlag)
			{
				return tagElement;
			}
			String newElementName = ((PsiFile) element).getName();
			final int index = newElementName.lastIndexOf('.');

			// TODO: need to evaluate new ns prefix
			newElementName = prependNamespacePrefix(newElementName.substring(0, index), tagElement.getNamespacePrefix());

			return getTagElement().setName(newElementName);
		}

		final XmlTag tag = getTagElement();
		throw new consulo.language.util.IncorrectOperationException("Cant bind to not a xml element definition!" + element + "," + metaData + "," + tag + "," + (tag != null ? tag.getDescriptor() : "unknown descriptor"));
	}

	@Override
	public boolean isReferenceTo(PsiElement element)
	{
		return getElement().getManager().areElementsEquivalent(element, resolve());
	}

	@Override
	@Nonnull
	public Object[] getVariants()
	{
		return ArrayUtil.EMPTY_OBJECT_ARRAY;
	}

	@Override
	public boolean isSoft()
	{
		return false;
	}

	@Nullable
	static TagNameReference createTagNameReference(XmlElement element, @Nonnull ASTNode nameElement, boolean startTagFlag)
	{
		final XmlExtension extension = XmlExtension.getExtensionByElement(element);
		return extension == null ? null : extension.createTagNameReference(nameElement, startTagFlag);
	}

	public boolean isStartTagFlag()
	{
		return myStartTagFlag;
	}
}
