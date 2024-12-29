package consulo.xml.dom;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 03-Aug-22
 */
public interface ImplementationProvider<Base, Impl extends Base>
{
	@Nonnull
	Class<Base> getInterfaceClass();

	@Nonnull
	Class<Impl> getImplementationClass();
}
