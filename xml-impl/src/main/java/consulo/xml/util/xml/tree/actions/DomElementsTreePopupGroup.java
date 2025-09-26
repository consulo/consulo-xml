package consulo.xml.util.xml.tree.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-09-26
 */
@ActionImpl(
    id = "DomElementsTreeView.TreePopup",
    children = {
        @ActionRef(type = GotoDomElementDeclarationAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = AddDomElementActionGroup.class),
        @ActionRef(type = DeleteDomElement.class)
    }
)
public class DomElementsTreePopupGroup extends DefaultActionGroup implements DumbAware {
    public DomElementsTreePopupGroup() {
        super(LocalizeValue.empty(), false);
    }
}
