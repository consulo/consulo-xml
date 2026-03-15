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
package consulo.xml.codeInspection.htmlInspections;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

/**
 * @author spleaner
 */
public class AddCustomTagOrAttributeIntentionAction implements LocalQuickFix {
    private final String myName;
    private final LocalizeValue myText;
    private final String myInspectionShortName;

    public AddCustomTagOrAttributeIntentionAction(
        String inspectionShortName,
        String name,
        LocalizeValue text
    ) {
        myInspectionShortName = inspectionShortName;
        myName = name;
        myText = text;
    }

    @Override
    public LocalizeValue getName() {
        return myText;
    }

    @Override
    @RequiredReadAction
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();

        InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        profile.<LocalInspectionTool, BaseXmlEntitiesInspectionState>modifyToolSettings(
            myInspectionShortName,
            element,
            (tool, state) -> state.addEntry(myName)
        );
    }
}
