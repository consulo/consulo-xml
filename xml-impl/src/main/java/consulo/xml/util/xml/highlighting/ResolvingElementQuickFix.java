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

import consulo.annotation.access.RequiredReadAction;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    public ResolvingElementQuickFix(
        Class<? extends DomElement> clazz,
        String newName,
        List<DomElement> parents,
        DomCollectionChildDescription childDescription
    ) {
        myClazz = clazz;
        myNewName = newName;
        myParents = parents;
        myChildDescription = childDescription;

        myTypeName = TypePresentationService.getInstance().getTypeNameOrStub(myClazz);
    }

    public void setTypeName(String typeName) {
        myTypeName = typeName;
    }

    @Nonnull
    @Override
    public String getName() {
        return DomBundle.message("create.new.element", myTypeName, myNewName);
    }

    @Nonnull
    @Override
    public String getText() {
        return getName();
    }

    @Nonnull
    @Override
    public String getFamilyName() {
        return DomBundle.message("quick.fixes.family");
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        applyFix();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        applyFix();
    }

    private void applyFix() {
        chooseParent(
            myParents,
            parent -> new WriteCommandAction.Simple(parent.getManager().getProject(), DomUtil.getFile(parent)) {
                protected void run() throws Throwable {
                    doFix(parent, myChildDescription, myNewName);
                }
            }.execute()
        );
    }

    protected DomElement doFix(DomElement parent, DomCollectionChildDescription childDescription, String newName) {
        DomElement domElement = childDescription.addValue(parent);
        GenericDomValue nameDomElement = domElement.getGenericInfo().getNameDomElement(domElement);
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
                    @Override
                    public PopupStep onChosen(DomElement selectedValue, boolean finalChoice) {
                        onChoose.accept(selectedValue);
                        return super.onChosen(selectedValue, finalChoice);
                    }

                    @Override
                    @RequiredReadAction
                    public Image getIconFor(DomElement aValue) {
                        return IconDescriptorUpdaters.getIcon(DomUtil.getFile(aValue), 0);
                    }

                    @Nonnull
                    @Override
                    @RequiredReadAction
                    public String getTextFor(DomElement value) {
                        String name = DomUtil.getFile(value).getName();
                        assert name != null;
                        return name;
                    }
                }).showInBestPositionFor(DataManager.getInstance().getDataContext());
        }
    }

    @Nullable
    public static <T extends DomElement> DomCollectionChildDescription getChildDescription(List<DomElement> contexts, Class<T> clazz) {
        if (contexts.size() == 0) {
            return null;
        }
        DomElement context = contexts.get(0);
        DomGenericInfo genericInfo = context.getGenericInfo();
        List<? extends DomCollectionChildDescription> descriptions = genericInfo.getCollectionChildrenDescriptions();
        for (DomCollectionChildDescription description : descriptions) {
            Type type = description.getType();
            if (type.equals(clazz)) {
                return description;
            }
        }
        return null;
    }

    @Nullable
    public static ResolvingElementQuickFix createFix(String newName, Class<? extends DomElement> clazz, DomElement scope) {
        List<DomElement> parents = ModelMergerUtil.getImplementations(scope);
        return createFix(newName, clazz, parents);
    }

    @Nullable
    public static ResolvingElementQuickFix createFix(String newName, Class<? extends DomElement> clazz, List<DomElement> parents) {
        DomCollectionChildDescription childDescription = getChildDescription(parents, clazz);
        if (newName.length() > 0 && childDescription != null) {
            return new ResolvingElementQuickFix(clazz, newName, parents, childDescription);
        }
        return null;
    }

    public static LocalQuickFix[] createFixes(String newName, Class<? extends DomElement> clazz, DomElement scope) {
        LocalQuickFix fix = createFix(newName, clazz, scope);
        return fix != null ? new LocalQuickFix[]{fix} : LocalQuickFix.EMPTY_ARRAY;
    }
}
