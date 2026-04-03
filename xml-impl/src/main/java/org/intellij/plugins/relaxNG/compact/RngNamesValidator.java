package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.project.Project;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;


@ExtensionImpl
public class RngNamesValidator implements NamesValidator
{
	@Override
	public boolean isKeyword(String name, Project project)
	{
		return RenameUtil.isKeyword(name);
	}

	@Override
	public boolean isIdentifier(String name, Project project)
	{
		return RenameUtil.isIdentifier(name);
	}

	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
