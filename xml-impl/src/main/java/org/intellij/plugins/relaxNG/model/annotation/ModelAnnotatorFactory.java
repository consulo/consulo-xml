package org.intellij.plugins.relaxNG.model.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import org.intellij.plugins.relaxNG.compact.RngCompactLanguage;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class ModelAnnotatorFactory implements AnnotatorFactory
{
	@Nullable
	@Override
	public Annotator createAnnotator()
	{
		return new ModelAnnotator();
	}

	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
