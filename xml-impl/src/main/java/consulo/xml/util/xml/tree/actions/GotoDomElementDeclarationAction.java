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

import consulo.ui.ex.action.AnActionEvent;
import consulo.xml.util.xml.tree.DomModelTreeView;
import consulo.xml.util.xml.tree.BaseDomElementNode;
import consulo.xml.util.xml.DomElementsNavigationManager;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementNavigationProvider;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.action.ActionsBundle;

/**
 * @author Sergey.Vasiliev
 */
public class GotoDomElementDeclarationAction extends BaseDomTreeAction {

  public void actionPerformed(AnActionEvent e, DomModelTreeView treeView) {
    final SimpleNode simpleNode = treeView.getTree().getSelectedNode();

    if(simpleNode instanceof BaseDomElementNode) {
      final DomElement domElement = ((BaseDomElementNode)simpleNode).getDomElement();
      final DomElementNavigationProvider provider =
        DomElementsNavigationManager.getManager(domElement.getManager().getProject()).getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME);

      provider.navigate(domElement, true);

    }
  }

  public void update(AnActionEvent e, DomModelTreeView treeView) {
    e.getPresentation().setVisible(treeView.getTree().getSelectedNode() instanceof BaseDomElementNode);
    e.getPresentation().setText(ActionsBundle.message("action.EditSource.text"));
  }
}
