package consulo.xml.patterns;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.inject.advanced.pattern.PatternClassProvider;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 06-Aug-22
 */
@ExtensionImpl
public class XmlPatternsProvider implements PatternClassProvider
{
	@Nullable
	@Override
	public String getAlias()
	{
		return "xml";
	}

	@Override
	public Class<?> getPatternClass()
	{
		return XmlPatterns.class;
	}
}
