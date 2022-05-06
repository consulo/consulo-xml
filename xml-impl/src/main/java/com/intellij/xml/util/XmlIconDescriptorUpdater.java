/*
 * Copyright 2013 Consulo.org
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

import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.language.icon.IconDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:47/19.07.13
 */
public class XmlIconDescriptorUpdater implements IconDescriptorUpdater
{
	private static final String XSD_FILE_EXTENSION = "xsd";
	private static final String WSDL_FILE_EXTENSION = "wsdl";

	@RequiredReadAction
	@Override
	public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags)
	{
		if(element instanceof XmlFile)
		{
			final VirtualFile vf = ((XmlFile) element).getVirtualFile();
			if(vf != null)
			{
				final String extension = vf.getExtension();

				if(XSD_FILE_EXTENSION.equals(extension))
				{
					iconDescriptor.setMainIcon(AllIcons.FileTypes.XsdFile);
				}
				if(WSDL_FILE_EXTENSION.equals(extension))
				{
					iconDescriptor.setMainIcon(AllIcons.FileTypes.WsdlFile);
				}
			}
		}
		else if(element instanceof XmlTag)
		{
			iconDescriptor.setMainIcon(PlatformIconGroup.nodesTag());
		}
	}
}
