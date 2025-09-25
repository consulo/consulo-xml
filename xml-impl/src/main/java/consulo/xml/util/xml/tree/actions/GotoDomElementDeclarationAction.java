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

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.xml.util.xml.tree.DomModelTreeView;
import consulo.xml.util.xml.tree.BaseDomElementNode;
import consulo.xml.util.xml.DomElementsNavigationManager;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementNavigationProvider;

/**
 * @author Sergey.Vasiliev
 */
@ActionImpl(id = "DomElementsTreeView.GotoDomElementDeclarationAction", shortcutFrom = @ActionRef(id = IdeActions.ACTION_EDIT_SOURCE))
public class GotoDomElementDeclarationAction extends BaseDomTreeAction {
    public GotoDomElementDeclarationAction() {
        super(ActionLocalize.actionEditsourceText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e, DomModelTreeView treeView) {
        if (treeView.getTree().getSelectedNode() instanceof BaseDomElementNode baseDomNode) {
            DomElement domElement = baseDomNode.getDomElement();
            DomElementNavigationProvider provider = DomElementsNavigationManager.getManager(domElement.getManager().getProject())
                .getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME);

            provider.navigate(domElement, true);
        }
    }

    @Override
    public void update(AnActionEvent e, DomModelTreeView treeView) {
        e.getPresentation().setVisible(treeView.getTree().getSelectedNode() instanceof BaseDomElementNode);
    }
}
