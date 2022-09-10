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

import com.intellij.xml.XmlBundle;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileEntry;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author Maxim.Mossienko
 */
public class AddHtmlTagOrAttributeToCustomsIntention implements IntentionAction {
  private final String myName;
  private final String myText;
  private final Key<InspectionProfileEntry> myInspectionKey;

  public AddHtmlTagOrAttributeToCustomsIntention(Key<InspectionProfileEntry> inspectionKey, String name, String text) {
    myInspectionKey = inspectionKey;
    myName = name;
    myText = text;
  }

  @Override
  @Nonnull
  public String getText() {
    return myText;
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return XmlBundle.message("fix.html.family");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, final PsiFile file) throws IncorrectOperationException {
    InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    profile.modifyToolSettings(myInspectionKey, file, new Consumer<InspectionProfileEntry>() {
      @Override
      public void accept(InspectionProfileEntry entry) {
        XmlEntitiesInspection xmlEntitiesInspection = (XmlEntitiesInspection) entry;
        xmlEntitiesInspection.addEntry(myName);
      }
    });
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
