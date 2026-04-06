package consulo.xml.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.ast.IElementType;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface StartTagEndTokenProvider {
    IElementType[] getTypes();
}
