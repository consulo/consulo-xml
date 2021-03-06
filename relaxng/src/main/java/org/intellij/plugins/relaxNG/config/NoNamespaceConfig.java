/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 26.11.2007
 */
public abstract class NoNamespaceConfig
{
	@Nullable
	public abstract String getMapping(@Nonnull PsiFile file);

	public abstract VirtualFile getMappedFile(@Nonnull PsiFile file);

	public abstract void setMapping(@Nonnull PsiFile file, @Nullable String location);

	public static NoNamespaceConfig getInstance(@Nonnull Project project)
	{
		return project.getComponent(NoNamespaceConfig.class);
	}
}
