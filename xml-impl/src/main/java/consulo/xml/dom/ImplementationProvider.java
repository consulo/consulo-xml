package consulo.xml.dom;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 03-Aug-22
 */
public interface ImplementationProvider<Result>
{
	@Nonnull
	Class<?> getInterfaceClass();

	@Nonnull
	Class<Result> getImplementationClass();
}
