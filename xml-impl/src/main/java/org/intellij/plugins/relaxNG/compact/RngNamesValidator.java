package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.project.Project;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;

import javax.annotation.Nonnull;

@ExtensionImpl
public class RngNamesValidator implements NamesValidator
{
	@Override
	public boolean isKeyword(@Nonnull String name, Project project)
	{
		return RenameUtil.isKeyword(name);
	}

	@Override
	public boolean isIdentifier(@Nonnull String name, Project project)
	{
		return RenameUtil.isIdentifier(name);
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
