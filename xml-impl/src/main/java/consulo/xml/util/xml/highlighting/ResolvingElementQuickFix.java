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

package consulo.xml.util.xml.highlighting;

import consulo.application.presentation.TypePresentationService;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
public class ResolvingElementQuickFix implements LocalQuickFix, IntentionAction {

  private final Class<? extends DomElement> myClazz;
  private final String myNewName;
  private final List<DomElement> myParents;
  private final DomCollectionChildDescription myChildDescription;
  private String myTypeName;

  public ResolvingElementQuickFix(final Class<? extends DomElement> clazz, final String newName, final List<DomElement> parents,
                                  final DomCollectionChildDescription childDescription) {
    myClazz = clazz;
    myNewName = newName;
    myParents = parents;
    myChildDescription = childDescription;

    myTypeName = TypePresentationService.getInstance().getTypeNameOrStub(myClazz);
  }

  public void setTypeName(final String typeName) {
    myTypeName = typeName;
  }

  @Nonnull
  public String getName() {
    return DomBundle.message("create.new.element", myTypeName, myNewName);
  }

  @Nonnull
  public String getText() {
    return getName();
  }

  @Nonnull
  public String getFamilyName() {
    return DomBundle.message("quick.fixes.family");
  }

  public boolean isAvailable(@Nonnull final Project project, final Editor editor, final PsiFile file) {
    return true;
  }

  public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    applyFix();
  }

  public boolean startInWriteAction() {
    return false;
  }

  public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
    applyFix();
  }

  private void applyFix() {
    chooseParent(myParents, new Consumer<DomElement>() {
      public void accept(final DomElement parent) {
        new WriteCommandAction.Simple(parent.getManager().getProject(), DomUtil.getFile(parent)) {
          protected void run() throws Throwable {
            doFix(parent, myChildDescription, myNewName);
          }
        }.execute();
      }
    });
  }

  protected DomElement doFix(DomElement parent, final DomCollectionChildDescription childDescription, String newName) {
    final DomElement domElement = childDescription.addValue(parent);
    final GenericDomValue nameDomElement = domElement.getGenericInfo().getNameDomElement(domElement);
    assert nameDomElement != null;
    nameDomElement.setStringValue(newName);
    return domElement;
  }

  protected static void chooseParent(final List<DomElement> files, final Consumer<DomElement> onChoose) {
    switch (files.size()) {
      case 0:
        return;
      case 1:
        onChoose.accept(files.iterator().next());
        return;
      default:
        JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<DomElement>(DomBundle.message("choose.file"), files) {
          public PopupStep onChosen(final DomElement selectedValue, final boolean finalChoice) {
            onChoose.accept(selectedValue);
            return super.onChosen(selectedValue, finalChoice);
          }

          public Image getIconFor(final DomElement aValue) {
            return IconDescriptorUpdaters.getIcon(DomUtil.getFile(aValue), 0);
          }

          @Nonnull
          public String getTextFor(final DomElement value) {
            final String name = DomUtil.getFile(value).getName();
            assert name != null;
            return name;
          }
        }).showInBestPositionFor(DataManager.getInstance().getDataContext());
    }
  }

  @Nullable
  public static <T extends DomElement> DomCollectionChildDescription getChildDescription(final List<DomElement> contexts, Class<T> clazz) {

    if (contexts.size() == 0) {
      return null;
    }
    final DomElement context = contexts.get(0);
    final DomGenericInfo genericInfo = context.getGenericInfo();
    final List<? extends DomCollectionChildDescription> descriptions = genericInfo.getCollectionChildrenDescriptions();
    for (DomCollectionChildDescription description : descriptions) {
      final Type type = description.getType();
      if (type.equals(clazz)) {
        return description;
      }
    }
    return null;
  }

  @Nullable
  public static ResolvingElementQuickFix createFix(final String newName, final Class<? extends DomElement> clazz, final DomElement scope) {
    final List<DomElement> parents = ModelMergerUtil.getImplementations(scope);
    return createFix(newName, clazz, parents);
  }

  @Nullable
  public static ResolvingElementQuickFix createFix(final String newName, final Class<? extends DomElement> clazz, final List<DomElement> parents) {
    final DomCollectionChildDescription childDescription = getChildDescription(parents, clazz);
    if (newName.length() > 0 && childDescription != null) {
      return new ResolvingElementQuickFix(clazz, newName, parents, childDescription);
    }
    return null;
  }

  public static LocalQuickFix[] createFixes(final String newName, Class<? extends DomElement> clazz, final DomElement scope) {
    final LocalQuickFix fix = createFix(newName, clazz, scope);
    return fix != null ? new LocalQuickFix[]{fix} : LocalQuickFix.EMPTY_ARRAY;
  }
}
