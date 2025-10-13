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

import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author Maxim.Mossienko
 */
public class AddHtmlTagOrAttributeToCustomsIntention implements SyntheticIntentionAction {
    @Nonnull
    private final String myName;
    @Nonnull
    private final LocalizeValue myText;
    @Nonnull
    private final String myInspectionKey;

    public AddHtmlTagOrAttributeToCustomsIntention(
        @Nonnull String inspectionKey,
        @Nonnull String name,
        @Nonnull LocalizeValue text
    ) {
        myInspectionKey = inspectionKey;
        myName = name;
        myText = text;
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        profile.<LocalInspectionTool, BaseXmlEntitiesInspectionState>modifyToolSettings(
            myInspectionKey,
            file,
            (tool, state) -> state.addEntry(myName)
        );
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
