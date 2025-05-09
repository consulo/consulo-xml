/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlFile;

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RelatedToHtmlFilesContributor
{
	public static final ExtensionPointName<RelatedToHtmlFilesContributor> EP_NAME = ExtensionPointName.create(RelatedToHtmlFilesContributor.class);

	public abstract void fillRelatedFiles(@Nonnull XmlFile xmlFile, @Nonnull Set<PsiFile> resultSet);

	public abstract String getGroupName();
}
