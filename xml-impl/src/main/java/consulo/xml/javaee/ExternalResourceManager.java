/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.xml.javaee;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import consulo.ide.ServiceManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.psi.PsiFile;

public abstract class ExternalResourceManager extends SimpleModificationTracker
{
	public static ExternalResourceManager getInstance()
	{
		return ServiceManager.getService(ExternalResourceManager.class);
	}

	public abstract void addResource(@Nonnull @NonNls String url, @NonNls String location);

	public abstract void addResource(@Nonnull @NonNls String url, @NonNls @Nullable String version, @NonNls String location);

	public abstract void removeResource(@Nonnull String url);

	public abstract void removeResource(@Nonnull String url, @Nullable String version);

	/**
	 * @see #getResourceLocation(String, Project)
	 */
	@Deprecated
	public abstract String getResourceLocation(@Nonnull @NonNls String url);

	public abstract String getResourceLocation(@Nonnull @NonNls String url, @Nullable String version);

	public abstract String getResourceLocation(@Nonnull @NonNls String url, @Nonnull Project project);

	@Nullable
	public abstract PsiFile getResourceLocation(@Nonnull @NonNls String url, @Nonnull PsiFile baseFile, @Nullable String version);

	public abstract String[] getResourceUrls(@Nullable FileType fileType, boolean includeStandard);

	public abstract String[] getResourceUrls(@Nullable FileType fileType, @NonNls @Nullable String version, boolean includeStandard);
}
