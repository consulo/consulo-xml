/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.xml.actions;

import com.intellij.xml.actions.validate.ValidateXmlAction;
import com.intellij.xml.actions.xmlbeans.GenerateInstanceDocumentFromSchemaAction;
import com.intellij.xml.actions.xmlbeans.GenerateSchemaFromInstanceDocumentAction;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.xml.codeInsight.actions.GenerateDTDAction;
import consulo.xml.localize.XmlLocalize;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(
    id = "ToolsXmlGroup",
    children = {
        @ActionRef(type = ValidateXmlAction.class),
        @ActionRef(type = GenerateDTDAction.class),
        @ActionRef(type = GenerateSchemaFromInstanceDocumentAction.class),
        @ActionRef(type = GenerateInstanceDocumentFromSchemaAction.class),
        @ActionRef(type = AnSeparator.class)
    },
    parents = @ActionParentRef(
            value = @ActionRef(id = IdeActions.TOOLS_MENU),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = IdeActions.GROUP_EXTERNAL_TOOLS)
    )
)
public class XmlActionsGroup extends DefaultActionGroup implements DumbAware {
    public XmlActionsGroup() {
        super(XmlLocalize.groupToolsText(), true);
    }
}
