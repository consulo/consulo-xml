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

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.psi.xml.*;
import com.intellij.xml.XmlBundle;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;

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

		OpenFileDescriptor openDescriptor = OpenFileDescriptorFactory.getInstance(project).builder(containingFile.getVirtualFile()).offset(anchorOffset).build();
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
