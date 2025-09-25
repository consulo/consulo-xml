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
package consulo.xml.util.xml.tree.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.presentation.TypePresentationService;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.MergedObject;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.tree.BaseDomElementNode;
import consulo.xml.util.xml.tree.DomElementsGroupNode;
import consulo.xml.util.xml.tree.DomModelTreeView;
import consulo.xml.util.xml.ui.actions.AddDomElementAction;
import consulo.xml.util.xml.ui.actions.DefaultAddAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Sergey.Vasiliev
 */
@ActionImpl(id = "DomElementsTreeView.AddElement")
public class AddElementInCollectionAction extends AddDomElementAction {
    private DomModelTreeView myTreeView;

    @Inject
    public AddElementInCollectionAction() {
    }

    public AddElementInCollectionAction(DomModelTreeView treeView) {
        myTreeView = treeView;
    }

    protected DomModelTreeView getTreeView(AnActionEvent e) {
        if (myTreeView != null) {
            return myTreeView;
        }

        return e.getData(DomModelTreeView.DATA_KEY);
    }

    @Override
    protected boolean isEnabled(AnActionEvent e) {
        DomModelTreeView treeView = getTreeView(e);

        boolean enabled = treeView != null;
        e.getPresentation().setEnabled(enabled);

        return enabled;
    }


    @Override
    protected void showPopup(ListPopup groupPopup, AnActionEvent e) {
        if (myTreeView == null) {
            if (e.getPlace().equals(DomModelTreeView.DOM_MODEL_TREE_VIEW_POPUP)) {
                groupPopup.showInCenterOf(getTreeView(e).getTree());
            }
            else {
                groupPopup.showInBestPositionFor(e.getDataContext());
            }
        }
        else {
            super.showPopup(groupPopup, e);
        }
    }

    @Nonnull
    @Override
    protected DomCollectionChildDescription[] getDomCollectionChildDescriptions(AnActionEvent e) {
        DomModelTreeView view = getTreeView(e);

        SimpleNode node = view.getTree().getSelectedNode();
        if (node instanceof BaseDomElementNode baseDomNode) {
            List<DomCollectionChildDescription> consolidated = baseDomNode.getConsolidatedChildrenDescriptions();
            if (consolidated.size() > 0) {
                return consolidated.toArray(new DomCollectionChildDescription[consolidated.size()]);
            }
        }

        DomElementsGroupNode groupNode = getDomElementsGroupNode(view);

        return groupNode == null
            ? DomCollectionChildDescription.EMPTY_ARRAY
            : new DomCollectionChildDescription[]{groupNode.getChildDescription()};
    }

    @Override
    protected DomElement getParentDomElement(AnActionEvent e) {
        DomModelTreeView view = getTreeView(e);
        SimpleNode node = view.getTree().getSelectedNode();
        if (node instanceof BaseDomElementNode baseDomNode) {
            if (baseDomNode.getConsolidatedChildrenDescriptions().size() > 0) {
                return baseDomNode.getDomElement();
            }
        }
        DomElementsGroupNode groupNode = getDomElementsGroupNode(view);

        return groupNode == null ? null : groupNode.getDomElement();
    }

    @Override
    protected JComponent getComponent(AnActionEvent e) {
        return getTreeView(e);
    }

    @Override
    protected boolean showAsPopup() {
        return true;
    }

    @Override
    protected String getActionText(AnActionEvent e) {
        String text = ApplicationLocalize.actionAdd().get();
        if (e.getPresentation().isEnabled()) {
            DomElementsGroupNode selectedNode = getDomElementsGroupNode(getTreeView(e));
            if (selectedNode != null) {
                Type type = selectedNode.getChildDescription().getType();

                text += " " + TypePresentationService.getInstance().getTypeName(ReflectionUtil.getRawType(type));
            }
        }
        return text;
    }

    @Nullable
    private static DomElementsGroupNode getDomElementsGroupNode(DomModelTreeView treeView) {
        SimpleNode simpleNode = treeView.getTree().getSelectedNode();
        while (simpleNode != null) {
            if (simpleNode instanceof DomElementsGroupNode groupNode) {
                return groupNode;
            }

            simpleNode = simpleNode.getParent();
        }
        return null;
    }

    @Override
    @RequiredReadAction
    protected AnAction createAddingAction(
        AnActionEvent e,
        String name,
        Image icon,
        Type type,
        DomCollectionChildDescription description
    ) {
        DomElement parentDomElement = getParentDomElement(e);

        if (parentDomElement instanceof MergedObject mergedObject) {
            List<DomElement> implementations = mergedObject.getImplementations();
            DefaultActionGroup actionGroup = new DefaultActionGroup(name, true);

            for (DomElement implementation : implementations) {
                XmlFile xmlFile = DomUtil.getFile(implementation);
                actionGroup.add(new MyDefaultAddAction(
                    implementation,
                    xmlFile.getName(),
                    IconDescriptorUpdaters.getIcon(xmlFile, 0),
                    e,
                    type,
                    description
                ));
            }
            return actionGroup;
        }

        return new MyDefaultAddAction(parentDomElement, name, icon, e, type, description);
    }

    private class MyDefaultAddAction extends DefaultAddAction {
        // we need this properties, don't remove it (shared dataContext assertion)
        private final DomElement myParent;
        private final DomModelTreeView myView;
        private final Type myType;
        private final DomCollectionChildDescription myDescription;

        public MyDefaultAddAction(
            DomElement parent,
            String name,
            Image icon,
            AnActionEvent e,
            Type type,
            DomCollectionChildDescription description
        ) {
            super(name, name, icon);
            myType = type;
            myDescription = description;
            myParent = parent;
            myView = getTreeView(e);
        }

        @Override
        protected Type getElementType() {
            return myType;
        }

        @Override
        protected DomCollectionChildDescription getDomCollectionChildDescription() {
            return myDescription;
        }

        @Override
        protected DomElement getParentDomElement() {
            return myParent;
        }

        @Override
        protected void afterAddition(DomElement newElement) {
            DomElement copy = newElement.createStableCopy();

            Application.get().invokeLater(() -> myView.setSelectedDomElement(copy));
        }
    }
}
