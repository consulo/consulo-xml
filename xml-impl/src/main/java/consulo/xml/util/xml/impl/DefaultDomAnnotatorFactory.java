package consulo.xml.util.xml.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.xml.language.XMLLanguage;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class DefaultDomAnnotatorFactory implements AnnotatorFactory
{
	@Nullable
	@Override
	public Annotator createAnnotator()
	{
		return new DefaultDomAnnotator();
	}

	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
