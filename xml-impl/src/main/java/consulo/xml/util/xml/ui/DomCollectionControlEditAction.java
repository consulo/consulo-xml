package consulo.xml.util.xml.ui;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.localize.ApplicationLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CommonActionsPanel;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-09-26
 */
@ActionImpl(id = "DomCollectionControl.Edit", shortcutFrom = @ActionRef(id = IdeActions.ACTION_EDIT_SOURCE))
public class DomCollectionControlEditAction extends AnAction {
    public DomCollectionControlEditAction() {
        super(ApplicationLocalize.actionEdit(), LocalizeValue.empty(), DomCollectionControl.EDIT_ICON);
        setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.EDIT));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
        control.doEdit();
        control.reset();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
        boolean visible = control != null && control.isEditable();
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && control.getComponent().getTable().getSelectedRowCount() == 1);
    }
}