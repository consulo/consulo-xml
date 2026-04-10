package consulo.xml.impl.internal.advancedInject;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.inject.advanced.pattern.PatternClassProvider;
import consulo.xml.dom.pattern.DomPatterns;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 06-Aug-22
 */
@ExtensionImpl
public class DomPatternsProvider implements PatternClassProvider
{
	@Nullable
	@Override
	public String getAlias()
	{
		return "dom";
	}

	@Override
	public Class<?> getPatternClass()
	{
		return DomPatterns.class;
	}
}
