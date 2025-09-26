package consulo.xml.util.xml.ui;

import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.CommonActionsPanel;
import consulo.ui.image.Image;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.ui.actions.AddDomElementAction;
import consulo.xml.util.xml.ui.actions.DefaultAddAction;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.lang.reflect.Type;

/**
 * @author UNV
 * @since 2025-09-26
 */
@ActionImpl(id = "DomCollectionControl.Add")
public class DomCollectionControlAddAction extends AddDomElementAction {
    public DomCollectionControlAddAction() {
        setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD));
    }

    @Override
    protected boolean isEnabled(AnActionEvent e) {
        return getDomCollectionControl(e) != null;
    }

    protected DomCollectionControl getDomCollectionControl(AnActionEvent e) {
        return DomCollectionControl.getDomCollectionControl(e);
    }

    @Nonnull
    @Override
    protected DomCollectionChildDescription[] getDomCollectionChildDescriptions(AnActionEvent e) {
        return new DomCollectionChildDescription[]{getDomCollectionControl(e).getChildDescription()};
    }

    @Override
    protected DomElement getParentDomElement(AnActionEvent e) {
        return getDomCollectionControl(e).getDomElement();
    }

    @Override
    protected JComponent getComponent(AnActionEvent e) {
        return getDomCollectionControl(e).getComponent();
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        DomCollectionControl control = getDomCollectionControl(e);
        AnAction[] actions = control.createAdditionActions();
        return actions == null ? super.getChildren(e) : actions;
    }

    @Override
    protected DefaultAddAction createAddingAction(
        AnActionEvent e,
        String name,
        Image icon,
        Type type,
        DomCollectionChildDescription description
    ) {
        return getDomCollectionControl(e).createDefaultAction(name, icon, type);
    }
}