package consulo.xml.dom;


/**
 * @author VISTALL
 * @since 03-Aug-22
 */
public interface ImplementationProvider<Base, Impl extends Base>
{
	Class<Base> getInterfaceClass();

	Class<Impl> getImplementationClass();
}
