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

import javax.swing.*;

/**
 * @author UNV
 * @since 2025-09-26
 */
@ActionImpl(id = "DomCollectionControl.Remove", shortcutFrom = @ActionRef(id = IdeActions.ACTION_DELETE))
public class DomCollectionControlRemoveAction extends AnAction {
    public DomCollectionControlRemoveAction() {
        super(ApplicationLocalize.actionRemove(), LocalizeValue.empty(), DomCollectionControl.REMOVE_ICON);
        setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
        control.doRemove();
        control.reset();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean enabled;
        DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
        if (control != null) {
            JTable table = control.getComponent().getTable();
            enabled = table != null && table.getSelectedRowCount() > 0;
        }
        else {
            enabled = false;
        }
        e.getPresentation().setEnabled(enabled);
    }
}