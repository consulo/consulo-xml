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
import consulo.application.Result;
import consulo.application.localize.ApplicationLocalize;
import consulo.language.editor.WriteCommandAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.ElementPresentation;
import consulo.xml.util.xml.tree.BaseDomElementNode;
import consulo.xml.util.xml.tree.DomFileElementNode;
import consulo.xml.util.xml.tree.DomModelTreeView;
import jakarta.inject.Inject;

/**
 * @author Sergey.Vasiliev
 */
@ActionImpl(id = "DomElementsTreeView.DeleteElement", shortcutFrom = @ActionRef(id = IdeActions.ACTION_DELETE))
public class DeleteDomElement extends BaseDomTreeAction {
    @Inject
    public DeleteDomElement() {
        super(LocalizeValue.localizeTODO("Delete element"));
    }

    public DeleteDomElement(DomModelTreeView treeView) {
        super(LocalizeValue.localizeTODO("Delete element"), treeView);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e, DomModelTreeView treeView) {
        if (treeView.getTree().getSelectedNode() instanceof BaseDomElementNode baseDomNode) {
            if (baseDomNode instanceof DomFileElementNode) {
                e.getPresentation().setVisible(false);
                return;
            }

            final DomElement domElement = baseDomNode.getDomElement();

            int ret = Messages.showOkCancelDialog(
                getPresentationText(baseDomNode) + "?",
                ApplicationLocalize.actionRemove().get(),
                UIUtil.getQuestionIcon()
            );
            if (ret == 0) {
                new WriteCommandAction(domElement.getManager().getProject(), DomUtil.getFile(domElement)) {
                    @Override
                    protected void run(Result result) throws Throwable {
                        domElement.undefine();
                    }
                }.execute();
            }
        }
    }

    @Override
    public void update(AnActionEvent e, DomModelTreeView treeView) {
        SimpleNode selectedNode = treeView.getTree().getSelectedNode();

        if (selectedNode instanceof DomFileElementNode) {
            e.getPresentation().setVisible(false);
            return;
        }

        boolean enabled = false;
        if (selectedNode instanceof BaseDomElementNode baseDomNode) {
            DomElement domElement = baseDomNode.getDomElement();
            if (domElement.isValid() && DomUtil.hasXml(domElement) && !(domElement.getParent() instanceof DomFileElement)) {
                enabled = true;
                e.getPresentation().setTextValue(getPresentationText(baseDomNode));
            }
        }

        e.getPresentation().setEnabled(enabled);
        if (!enabled) {
            e.getPresentation().setTextValue(ApplicationLocalize.actionRemove());
        }
        e.getPresentation().setIcon(PlatformIconGroup.generalRemove());
    }

    private static LocalizeValue getPresentationText(BaseDomElementNode selectedNode) {
        String removeString = ApplicationLocalize.actionRemove().get();
        ElementPresentation presentation = selectedNode.getDomElement().getPresentation();
        removeString += " " + presentation.getTypeName() +
            (presentation.getElementName() == null || presentation.getElementName()
                .trim()
                .length() == 0 ? "" : ": " + presentation.getElementName());
        return LocalizeValue.localizeTODO(removeString);
    }
}
