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
import jakarta.annotation.Nonnull;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "DomElementsTreeView.AddElementGroup")
public class AddDomElementActionGroup extends ActionGroup {
    private final AddElementInCollectionAction myAction = new AddElementInCollectionAction() {
        @Override
        protected boolean showAsPopup() {
            return false;
        }
    };

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return myAction.getChildren(e);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        //myAction.getChildren(e).length
        getTemplatePresentation().setTextValue(myAction.getTemplatePresentation().getTextValue());
        super.update(e);
    }
}
