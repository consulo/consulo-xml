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

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationBundle;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.ElementPresentation;
import consulo.xml.util.xml.tree.BaseDomElementNode;
import consulo.xml.util.xml.tree.DomFileElementNode;
import consulo.xml.util.xml.tree.DomModelTreeView;

/**
 * User: Sergey.Vasiliev
 */
public class DeleteDomElement extends BaseDomTreeAction {

  public DeleteDomElement() {
  }

  public DeleteDomElement(final DomModelTreeView treeView) {
    super(treeView);
  }

  public void actionPerformed(AnActionEvent e, DomModelTreeView treeView) {
    final SimpleNode selectedNode = treeView.getTree().getSelectedNode();

    if (selectedNode instanceof BaseDomElementNode) {

      if (selectedNode instanceof DomFileElementNode) {
        e.getPresentation().setVisible(false);
        return;
      }
      
      final DomElement domElement = ((BaseDomElementNode)selectedNode).getDomElement();

      final int ret = Messages.showOkCancelDialog(getPresentationText(selectedNode) + "?", ApplicationBundle.message("action.remove"),
                                                  Messages.getQuestionIcon());
      if (ret == 0) {
      new WriteCommandAction(domElement.getManager().getProject(), DomUtil.getFile(domElement)) {
        protected void run(final Result result) throws Throwable {
          domElement.undefine();
        }
      }.execute();
      }
    }
  }

  public void update(AnActionEvent e, DomModelTreeView treeView) {
    final SimpleNode selectedNode = treeView.getTree().getSelectedNode();

    if (selectedNode instanceof DomFileElementNode) {
      e.getPresentation().setVisible(false);
      return;
    }

    boolean enabled = false;
    if (selectedNode instanceof BaseDomElementNode) {
      final DomElement domElement = ((BaseDomElementNode)selectedNode).getDomElement();
      if (domElement.isValid() && DomUtil.hasXml(domElement) && !(domElement.getParent() instanceof DomFileElement)) {
        enabled = true;
      }
    }

    e.getPresentation().setEnabled(enabled);


    if (enabled) {
      e.getPresentation().setText(getPresentationText(selectedNode));
    }
    else {
      e.getPresentation().setText(ApplicationBundle.message("action.remove"));
    }

    e.getPresentation().setIcon(AllIcons.General.Remove);
  }

  private static String getPresentationText(final SimpleNode selectedNode) {
    String removeString = ApplicationBundle.message("action.remove");
    final ElementPresentation presentation = ((BaseDomElementNode)selectedNode).getDomElement().getPresentation();
    removeString += " " + presentation.getTypeName() +
                                (presentation.getElementName() == null || presentation.getElementName().trim().length() == 0? "" : ": " + presentation.getElementName());
    return removeString;
  }
}
