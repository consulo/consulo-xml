package consulo.xml.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-09-25
 */
@ActionImpl(
    id = "NewXml",
    children = @ActionRef(type = CreateHtmlFileAction.class),
    parents = @ActionParentRef(
        value = @ActionRef(id = IdeActions.GROUP_NEW),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "NewFromTemplate")
    )
)
public class NewXmlGroup extends DefaultActionGroup implements DumbAware {
    public NewXmlGroup() {
        super(LocalizeValue.empty(), false);
    }
}
