/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.util.xml.ui.actions;

import consulo.application.localize.ApplicationLocalize;
import consulo.application.presentation.TypePresentationService;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CommonActionsPanel;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.ElementPresentationManager;
import consulo.xml.util.xml.TypeChooser;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.ui.DomCollectionControl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey.Vasiliev
 */
public abstract class AddDomElementAction extends AnAction {
    public AddDomElementAction() {
        super(ApplicationLocalize.actionAdd(), LocalizeValue.empty(), DomCollectionControl.ADD_ICON);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        if (!isEnabled(e)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        AnAction[] actions = getChildren(e);
        for (AnAction action : actions) {
            e.getPresentation().setEnabled(true);
            action.update(e);
            if (e.getPresentation().isEnabled()) {
                break;
            }
        }
        if (actions.length == 1) {
            e.getPresentation().setTextValue(actions[0].getTemplatePresentation().getTextValue());
        }
        else {
            String actionText = getActionText(e);
            if (!actionText.endsWith("...")) {
                e.getPresentation().setText(actionText + (actions.length > 1 ? "..." : ""));
            }
        }
        e.getPresentation().setIcon(DomCollectionControl.ADD_ICON);

        super.update(e);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        AnAction[] actions = getChildren(e);
        if (actions.length > 1) {
            DefaultActionGroup group = new DefaultActionGroup();
            for (AnAction action : actions) {
                group.add(action);
            }

            DataContext dataContext = e.getDataContext();
            ListPopup groupPopup =
                JBPopupFactory.getInstance().createActionGroupPopup(
                    null,
                    group,
                    dataContext,
                    JBPopupFactory.ActionSelectionAid.NUMBERING,
                    true
                );

            showPopup(groupPopup, e);
        }
        else if (actions.length == 1) {
            actions[0].actionPerformed(e);
        }
    }

    protected String getActionText(AnActionEvent e) {
        return e.getPresentation().getText();
    }

    protected boolean isEnabled(AnActionEvent e) {
        return true;
    }

    protected void showPopup(ListPopup groupPopup, AnActionEvent e) {
        Component component = e.getInputEvent().getComponent();

        if (component instanceof ActionButtonComponent) {
            groupPopup.showUnderneathOf(component);
        }
        else {
            groupPopup.showInBestPositionFor(e.getDataContext());
        }
    }

    @Nonnull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        Project project = e == null ? null : e.getData(Project.KEY);
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }

        DomCollectionChildDescription[] descriptions = getDomCollectionChildDescriptions(e);
        final List<AnAction> actions = new ArrayList<>();
        for (DomCollectionChildDescription description : descriptions) {
            TypeChooser chooser = DomManager.getDomManager(project).getTypeChooserManager().getTypeChooser(description.getType());
            for (Type type : chooser.getChooserTypes()) {

                Class<?> rawType = ReflectionUtil.getRawType(type);

                String name = TypePresentationService.getInstance().getTypeNameOrStub(rawType);
                Image icon = null;
                if (!showAsPopup() || descriptions.length == 1) {
                    //if (descriptions.length > 1) {
                    icon = ElementPresentationManager.getIconForClass(rawType);
                    //}
                }
                actions.add(createAddingAction(e, ApplicationLocalize.actionAdd() + " " + name, icon, type, description));
            }
        }
        if (actions.size() > 1 && showAsPopup()) {
            ActionGroup group = new ActionGroup() {
                @Nonnull
                @Override
                public AnAction[] getChildren(@Nullable AnActionEvent e) {
                    return actions.toArray(new AnAction[actions.size()]);
                }
            };
            return new AnAction[]{new ShowPopupAction(group)};
        }
        else if (actions.size() > 1) {
            actions.add(AnSeparator.getInstance());
        }
        else if (actions.size() == 1) {
        }
        return actions.toArray(new AnAction[actions.size()]);
    }

    protected abstract AnAction createAddingAction(
        AnActionEvent e,
        String name,
        Image icon,
        Type type,
        DomCollectionChildDescription description
    );

    @Nonnull
    protected abstract DomCollectionChildDescription[] getDomCollectionChildDescriptions(AnActionEvent e);

    @Nullable
    protected abstract DomElement getParentDomElement(AnActionEvent e);

    protected abstract JComponent getComponent(AnActionEvent e);

    protected boolean showAsPopup() {
        return true;
    }

    protected class ShowPopupAction extends AnAction {

        protected final ActionGroup myGroup;

        protected ShowPopupAction(ActionGroup group) {
            super(ApplicationLocalize.actionAdd(), LocalizeValue.empty(), DomCollectionControl.ADD_ICON);
            myGroup = group;
            setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            ListPopup groupPopup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                myGroup,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.NUMBERING,
                true
            );

            showPopup(groupPopup, e);
        }
    }
}
