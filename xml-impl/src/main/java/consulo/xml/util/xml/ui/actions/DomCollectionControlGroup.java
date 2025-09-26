package consulo.xml.util.xml.ui.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.xml.util.xml.ui.DomCollectionControlAddAction;
import consulo.xml.util.xml.ui.DomCollectionControlEditAction;
import consulo.xml.util.xml.ui.DomCollectionControlRemoveAction;

/**
 * @author UNV
 * @since 2025-09-26
 */
@ActionImpl(
    id = "DomCollectionControl",
    children = {
        @ActionRef(type = DomCollectionControlAddAction.class),
        @ActionRef(type = DomCollectionControlEditAction.class),
        @ActionRef(type = DomCollectionControlRemoveAction.class)
    }
)
public class DomCollectionControlGroup extends DefaultActionGroup implements DumbAware {
    public DomCollectionControlGroup() {
        super(LocalizeValue.empty(), false);
    }
}
