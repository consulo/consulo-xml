/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.maddyhome.idea.copyright.psi;

import javax.swing.JPanel;
import javax.xml.bind.annotation.XmlElementDecl;

import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.copyright.config.CopyrightFileConfig;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeDecl;
import com.intellij.psi.xml.XmlDoctype;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.util.HtmlUtil;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.ui.TemplateCommentPanel;

/**
 * User: anna
 * Date: 10/27/10
 */
public class UpdateXmlCopyrightsProvider extends UpdateCopyrightsProvider
{
	private static final String[] LOCATIONS_IN_FILE = new String[]{"Before Doctype", "Before Root Tag"};

	public static final int LOCATION_BEFORE_DOCTYPE = 1;
	public static final int LOCATION_BEFORE_ROOTTAG = 2;

	public static class UpdateXmlFileCopyright extends UpdatePsiFileCopyright
	{
		protected UpdateXmlFileCopyright(@NotNull PsiFile psiFile, @NotNull CopyrightProfile copyrightProfile)
		{
			super(psiFile, copyrightProfile);
		}

		@Override
		protected boolean accept()
		{
			return getFile() instanceof XmlFile;
		}

		@Override
		protected void scanFile()
		{
			logger.debug("updating " + getFile().getVirtualFile());


			XmlDoctype doctype = null;
			PsiElement root = null;

			XmlDocument doc = ((XmlFile) getFile()).getDocument();
			PsiElement elem = doc.getFirstChild();
			while(elem != null)
			{
				if(elem instanceof XmlProlog)
				{
					PsiElement prolog = elem.getFirstChild();
					while(prolog != null)
					{
						if(prolog instanceof XmlDoctype)
						{
							doctype = (XmlDoctype) prolog;
						}

						prolog = prolog.getNextSibling();
					}
				}
				else if(elem instanceof XmlTag || elem instanceof XmlElementDecl || elem instanceof XmlAttributeDecl)
				{
					root = elem;
					break;
				}

				elem = elem.getNextSibling();
			}

			PsiElement first = doc.getFirstChild();
			if(root == null)
			{
				root = doc.getLastChild();
			}

			int location = getLanguageOptions().getFileLocation();
			if(doctype != null && !isHtml5DoctypeIEFix(doc))
			{
				checkComments(first, doctype, location == LOCATION_BEFORE_DOCTYPE);
				first = doctype;
			}
			else if(location == LOCATION_BEFORE_DOCTYPE)
			{
				location = LOCATION_BEFORE_ROOTTAG;
			}

			if(root != null)
			{
				checkComments(first, root, location == LOCATION_BEFORE_ROOTTAG);
			}
			else if(location == LOCATION_BEFORE_ROOTTAG)
			{
				// If we get here we have an empty file
				checkComments(first, first, true);
			}
		}

		private boolean isHtml5DoctypeIEFix(XmlDocument doc)
		{
			if(HtmlUtil.isHtml5Document(doc))
			{
				return true; // IE goes quirks mode if comment before doc type so pardon the setting we will not handle you then
			}
			return false;
		}

		@Override
		protected PsiElement getPreviousSibling(PsiElement element)
		{
			if(element == null)
			{
				return null;
			}

			PsiElement res = element.getPrevSibling();
			if(res != null)
			{
				if(res instanceof XmlProlog)
				{
					XmlProlog prolog = (XmlProlog) res;
					if(prolog.getChildren().length > 0)
					{
						res = prolog.getLastChild();
					}
					else
					{
						res = prolog.getPrevSibling();
					}
				}
			}
			else
			{
				if(element.getParent() instanceof XmlProlog)
				{
					res = element.getParent().getPrevSibling();
				}
			}

			return res;
		}

		@Override
		protected PsiElement getNextSibling(PsiElement element)
		{
			if(element == null)
			{
				return null;
			}

			PsiElement res = element instanceof XmlProlog ? element : element.getNextSibling();
			if(res != null)
			{
				if(res instanceof XmlProlog)
				{
					XmlProlog prolog = (XmlProlog) res;
					if(prolog.getChildren().length > 0)
					{
						res = prolog.getFirstChild();
					}
					else
					{
						res = prolog.getNextSibling();
					}
				}
			}
			else
			{
				if(element.getParent() instanceof XmlProlog)
				{
					res = element.getParent().getNextSibling();
				}
			}

			return res;
		}

		private static final Logger logger = Logger.getInstance(UpdateXmlFileCopyright.class.getName());
	}

	@NotNull
	@Override
	public UpdatePsiFileCopyright createInstance(@NotNull PsiFile file, @NotNull CopyrightProfile copyrightProfile)
	{
		return new UpdateXmlFileCopyright(file, copyrightProfile);
	}

	@NotNull
	@Override
	public CopyrightFileConfig createDefaultOptions()
	{
		CopyrightFileConfig copyrightFileConfig = new CopyrightFileConfig();
		copyrightFileConfig.setPrefixLines(false);
		return copyrightFileConfig;
	}

	@NotNull
	@Override
	public TemplateCommentPanel createConfigurable(@NotNull Project project, @NotNull TemplateCommentPanel parentPane, @NotNull FileType fileType)
	{
		return new TemplateCommentPanel(fileType, parentPane, project)
		{
			@Override
			public void addAdditionalComponents(@NotNull JPanel additionalPanel)
			{
				addLocationInFile(LOCATIONS_IN_FILE);
			}
		};
	}
}
