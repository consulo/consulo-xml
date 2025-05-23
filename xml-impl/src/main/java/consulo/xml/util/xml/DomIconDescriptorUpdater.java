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
package consulo.xml.util.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.psi.xml.XmlFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 1:16/19.07.13
 */
@ExtensionImpl
public class DomIconDescriptorUpdater implements IconDescriptorUpdater
{
	@RequiredReadAction
	@Override
	public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags)
	{
		if(element instanceof XmlFile)
		{
			DomFileDescription<?> description = DomManager.getDomManager(element.getProject()).getDomFileDescription((XmlFile) element);
			if(description != null)
			{
				final Image fileIcon = description.getFileIcon(flags);
				if(fileIcon != null)
				{
					iconDescriptor.setMainIcon(fileIcon);
				}
			}
		}
	}
}
