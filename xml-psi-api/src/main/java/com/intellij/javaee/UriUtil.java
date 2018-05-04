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
package com.intellij.javaee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;

public class UriUtil
{
	private UriUtil()
	{
	}

	/**
	 * @see #findRelative(String, com.intellij.psi.PsiFileSystemItem)
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