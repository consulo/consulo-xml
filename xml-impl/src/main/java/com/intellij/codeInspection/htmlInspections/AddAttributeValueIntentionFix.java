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

package com.intellij.codeInspection.htmlInspections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;

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
		return XmlErrorMessages.message("add.attribute.value.quickfix.text");
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
