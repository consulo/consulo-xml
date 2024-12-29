package consulo.xml.psi.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.ast.IElementType;

import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface StartTagEndTokenProvider
{
	@Nonnull
	IElementType[] getTypes();
}
