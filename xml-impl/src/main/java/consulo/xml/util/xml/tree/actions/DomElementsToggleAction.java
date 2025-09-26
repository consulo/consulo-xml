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

import consulo.application.presentation.TypePresentationService;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.ElementPresentationManager;
import consulo.xml.util.xml.tree.BaseDomElementNode;
import consulo.xml.util.xml.tree.DomModelTreeView;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey.Vasiliev
 */
public class DomElementsToggleAction extends ToggleAction {
    private final DomModelTreeView myTreeView;
    private final Class myClass;
    private final Image myIcon;
    private final String myText;

    public DomElementsToggleAction(DomModelTreeView treeView, Class aClass) {
        myTreeView = treeView;
        myClass = aClass;

        Image icon = ElementPresentationManager.getIcon(myClass);
        if (icon == null) {
            icon = PlatformIconGroup.nodesProperty();
        }
        myIcon = icon;

        myText = TypePresentationService.getInstance().getTypeNameOrStub(myClass);

        if (getHiders() == null) {
            DomUtil.getFile(myTreeView.getRootElement())
                .putUserData(BaseDomElementNode.TREE_NODES_HIDERS_KEY, new HashMap<>());
        }

        if (getHiders().get(myClass) == null) {
            getHiders().put(myClass, true);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        e.getPresentation().setIcon(myIcon);
        e.getPresentation().setText((getHiders().get(myClass) ? "Hide " : "Show ") + myText);

        e.getPresentation().setEnabled(getHiders() != null && getHiders().get(myClass) != null);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return getHiders().get(myClass);
    }

    private Map<Class, Boolean> getHiders() {
        return DomUtil.getFile(myTreeView.getRootElement()).getUserData(BaseDomElementNode.TREE_NODES_HIDERS_KEY);
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        getHiders().put(myClass, state);
        myTreeView.getBuilder().updateFromRoot();
    }
}

