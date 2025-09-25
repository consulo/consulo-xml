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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.xml.util.xml.tree.DomModelTreeView;
import jakarta.annotation.Nonnull;

/**
 * @author Sergey.Vasiliev
 */
abstract public class BaseDomTreeAction extends AnAction {
    private DomModelTreeView myTreeView;

    protected BaseDomTreeAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected BaseDomTreeAction(@Nonnull LocalizeValue text, DomModelTreeView treeView) {
        super(text);
        myTreeView = treeView;
    }

    @Override
    final public void update(@Nonnull AnActionEvent e) {
        DomModelTreeView treeView = getTreeView(e);

        if (treeView != null) {
            update(e, treeView);
        }
        else {
            e.getPresentation().setEnabled(false);
        }
    }

    protected DomModelTreeView getTreeView(AnActionEvent e) {
        if (myTreeView != null) {
            return myTreeView;
        }

        return e.getData(DomModelTreeView.DATA_KEY);
    }

    @Override
    @RequiredUIAccess
    final public void actionPerformed(@Nonnull AnActionEvent e) {
        DomModelTreeView treeView = getTreeView(e);
        if (treeView != null) {
            actionPerformed(e, treeView);
        }
    }

    @RequiredUIAccess
    public abstract void actionPerformed(AnActionEvent e, DomModelTreeView treeView);

    public abstract void update(AnActionEvent e, DomModelTreeView treeView);
}

