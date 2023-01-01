package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Commenter;
import consulo.language.Language;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl
public class RngCommenter implements Commenter
{
	@Override
	@Nullable
	public String getLineCommentPrefix()
	{
		return "#";
	}

	@Override
	@Nullable
	public String getBlockCommentPrefix()
	{
		return null;
	}

	@Override
	@Nullable
	public String getBlockCommentSuffix()
	{
		return null;
	}

	@Override
	public String getCommentedBlockCommentPrefix()
	{
		return null;
	}

	@Override
	public String getCommentedBlockCommentSuffix()
	{
		return null;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
