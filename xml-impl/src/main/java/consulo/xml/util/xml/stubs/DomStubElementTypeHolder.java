package consulo.xml.util.xml.stubs;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.ObjectStubSerializerProvider;
import consulo.language.psi.stub.StubElementTypeHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author VISTALL
 * @since 06-Aug-22
 */
@ExtensionImpl
public class DomStubElementTypeHolder extends StubElementTypeHolder<DomElementTypeHolder>
{
	@Nullable
	@Override
	public String getExternalIdPrefix()
	{
		return "xml.";
	}

	@Nonnull
	@Override
	public List<ObjectStubSerializerProvider> loadSerializers()
	{
		return allFromStaticFields(DomElementTypeHolder.class, Field::get);
	}
}
