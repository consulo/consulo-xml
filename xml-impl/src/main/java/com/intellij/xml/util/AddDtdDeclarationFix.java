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
package com.intellij.xml.util;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttlistDecl;
import com.intellij.psi.xml.XmlConditionalSection;
import com.intellij.psi.xml.XmlDoctype;
import com.intellij.psi.xml.XmlElementDecl;
import com.intellij.psi.xml.XmlEntityDecl;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlMarkupDecl;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlBundle;

public class AddDtdDeclarationFix implements LocalQuickFix
{
	private final String myMessageKey;
	private final String myElementDeclarationName;
	private final String myReference;

	public AddDtdDeclarationFix(@PropertyKey(resourceBundle = XmlBundle.PATH_TO_BUNDLE) String messageKey, @Nonnull String elementDeclarationName, @Nonnull PsiReference reference)
	{
		myMessageKey = messageKey;
		myElementDeclarationName = elementDeclarationName;
		myReference = reference.getCanonicalText();
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return XmlBundle.message(myMessageKey, myReference);
	}

	@Override
	public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor)
	{
		final PsiElement element = descriptor.getPsiElement();
		final PsiFile containingFile = element.getContainingFile();

		@NonNls String prefixToInsert = "";
		@NonNls String suffixToInsert = "";

		final int UNDEFINED_OFFSET = -1;
		int anchorOffset = UNDEFINED_OFFSET;
		PsiElement anchor = PsiTreeUtil.getParentOfType(element, XmlElementDecl.class, XmlAttlistDecl.class, XmlEntityDecl.class, XmlConditionalSection.class);
		if(anchor != null)
		{
			anchorOffset = anchor.getTextRange().getStartOffset();
		}

		if(anchorOffset == UNDEFINED_OFFSET && containingFile.getLanguage() == XMLLanguage.INSTANCE)
		{
			XmlFile file = (XmlFile) containingFile;
			final XmlProlog prolog = file.getDocument().getProlog();
			assert prolog != null;

			final XmlDoctype doctype = prolog.getDoctype();
			final XmlMarkupDecl markupDecl;

			if(doctype != null)
			{
				markupDecl = doctype.getMarkupDecl();
			}
			else
			{
				markupDecl = null;
			}

			if(doctype == null)
			{
				final XmlTag rootTag = file.getDocument().getRootTag();
				prefixToInsert = "<!DOCTYPE " + ((rootTag != null) ? rootTag.getName() : "null");
				suffixToInsert = ">\n";
			}
			if(markupDecl == null)
			{
				prefixToInsert += " [\n";
				suffixToInsert = "]" + suffixToInsert;

				if(doctype != null)
				{
					anchorOffset = doctype.getTextRange().getEndOffset() - 1; // just before last '>'
				}
				else
				{
					anchorOffset = prolog.getTextRange().getEndOffset();
				}
			}
		}

		if(anchorOffset == UNDEFINED_OFFSET)
		{
			anchorOffset = element.getTextRange().getStartOffset();
		}

		OpenFileDescriptor openDescriptor = new OpenFileDescriptor(project, containingFile.getVirtualFile(), anchorOffset);
		final Editor editor = FileEditorManager.getInstance(project).openTextEditor(openDescriptor, true);
		final TemplateManager templateManager = TemplateManager.getInstance(project);
		final Template t = templateManager.createTemplate("", "");

		if(!prefixToInsert.isEmpty())
		{
			t.addTextSegment(prefixToInsert);
		}
		t.addTextSegment("<!" + myElementDeclarationName + " " + myReference + " ");
		t.addEndVariable();
		t.addTextSegment(">\n");
		if(!suffixToInsert.isEmpty())
		{
			t.addTextSegment(suffixToInsert);
		}
		templateManager.startTemplate(editor, t);
	}
}
