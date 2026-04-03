package consulo.xml.psi.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.ast.IElementType;


@ExtensionAPI(ComponentScope.APPLICATION)
public interface StartTagEndTokenProvider
{
	IElementType[] getTypes();
}
