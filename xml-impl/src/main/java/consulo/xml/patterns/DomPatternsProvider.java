package consulo.xml.patterns;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.inject.advanced.pattern.PatternClassProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

	@Nonnull
	@Override
	public Class<?> getPatternClass()
	{
		return DomPatterns.class;
	}
}
