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

import consulo.component.util.ModificationTracker;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ExternalResourceManager extends ModificationTracker
{
	@Deprecated
	@Nonnull
	public static ApplicationExternalResourceManager getInstance()
	{
		return ApplicationExternalResourceManager.getInstance();
	}

	void addResource(@Nonnull @NonNls String url, @NonNls String location);

	void addResource(@Nonnull @NonNls String url, @NonNls @Nullable String version, @NonNls String location);

	void removeResource(@Nonnull String url);

	void removeResource(@Nonnull String url, @Nullable String version);

	/**
	 * @see #getResourceLocation(String, Project)
	 */
	@Deprecated
	String getResourceLocation(@Nonnull @NonNls String url);

	String getResourceLocation(@Nonnull @NonNls String url, @Nullable String version);

	String getResourceLocation(@Nonnull @NonNls String url, @Nonnull Project project);

	@Nullable
	PsiFile getResourceLocation(@Nonnull @NonNls String url, @Nonnull PsiFile baseFile, @Nullable String version);

	String[] getResourceUrls(@Nullable FileType fileType, boolean includeStandard);

	String[] getResourceUrls(@Nullable FileType fileType, @NonNls @Nullable String version, boolean includeStandard);
}
