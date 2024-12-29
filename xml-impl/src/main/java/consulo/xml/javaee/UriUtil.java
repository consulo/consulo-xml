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

/*
 * @author max
 */
package consulo.xml.javaee;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFileSystemItem;

public class UriUtil
{
	private UriUtil()
	{
	}

	/**
	 * @see #findRelative(String, PsiFileSystemItem)
	 */
	@Deprecated
	@Nullable
	public static VirtualFile findRelativeFile(String uri, VirtualFile base)
	{
		return VfsUtilCore.findRelativeFile(ExternalResourceManager.getInstance().getResourceLocation(uri), base);
	}

	@Nullable
	public static VirtualFile findRelative(String uri, @Nonnull PsiFileSystemItem base)
	{
		String location = ExternalResourceManager.getInstance().getResourceLocation(uri, base.getProject());
		return VfsUtilCore.findRelativeFile(location, base.getVirtualFile());
	}

	// cannot use UriUtil.SLASH_MATCHER.trimFrom - we don't depend on guava
	@Nonnull
	public static String trimSlashFrom(@Nonnull String path)
	{
		return StringUtil.trimStart(StringUtil.trimEnd(path, "/"), "/");
	}
}