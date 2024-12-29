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

package consulo.xml.codeInspection.htmlInspections;

import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class AddAttributeValueIntentionFix extends LocalQuickFixAndIntentionActionOnPsiElement
{
	public AddAttributeValueIntentionFix(@Nullable PsiElement element)
	{
		super(element);
	}

	@Nonnull
	@Override
	public String getText()
	{
		return XmlErrorLocalize.addAttributeValueQuickfixText().get();
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return getName();
	}

	@Override
	public void invoke(@Nonnull Project project,
			@Nonnull PsiFile file,
			@Nullable final Editor editor,
			@Nonnull PsiElement startElement,
			@Nonnull PsiElement endElement)
	{
		final XmlAttribute attribute = PsiTreeUtil.getNonStrictParentOfType(startElement, XmlAttribute.class);
		if(attribute == null || attribute.getValue() != null)
		{
			return;
		}

		if(!FileModificationService.getInstance().prepareFileForWrite(attribute.getContainingFile()))
		{
			return;
		}

		new WriteCommandAction(project)
		{
			@Override
			protected void run(@Nonnull final Result result)
			{
				final XmlAttribute attributeWithValue = XmlElementFactory.getInstance(getProject()).createXmlAttribute(attribute.getName(), "");
				final PsiElement newAttribute = attribute.replace(attributeWithValue);

				if(editor != null && newAttribute != null && newAttribute instanceof XmlAttribute && newAttribute.isValid())
				{
					final XmlAttributeValue valueElement = ((XmlAttribute) newAttribute).getValueElement();
					if(valueElement != null)
					{
						editor.getCaretModel().moveToOffset(valueElement.getTextOffset());
						AutoPopupController.getInstance(newAttribute.getProject()).scheduleAutoPopup(editor);
					}
				}
			}
		}.execute();
	}
}
