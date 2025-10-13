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
package com.intellij.xml.refactoring;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.refactoring.TitledHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.rename.PsiElementRenameHandler;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 * @since 2007-08-07
 */
@ExtensionImpl(id = "xmlTagRenameHandler")
public class XmlTagRenameHandler implements RenameHandler, TitledHandler {
    private static final Logger LOG = Logger.getInstance(XmlTagRenameHandler.class);

    @Override
    public boolean isAvailableOnDataContext(final DataContext dataContext) {
        final PsiElement element = getElement(dataContext);
        if (element == null || PsiElementRenameHandler.isVetoed(element)) {
            return false;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof XmlTag)) {
            return false;
        }
        XmlTag tag = (XmlTag)parent;
        String prefix = tag.getNamespacePrefix();
        if (StringUtil.isNotEmpty(prefix)) {
            Editor editor = getEditor(dataContext);
            assert editor != null;
            int offset = editor.getCaretModel().getOffset();
            if (offset <= element.getTextRange().getStartOffset() + prefix.length()) {
                return false;
            }
        }
        //noinspection ConstantConditions
        return isDeclarationOutOfProjectOrAbsent(element.getProject(), dataContext);
    }

    @Override
    public boolean isRenaming(final DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }

    @Nonnull
    @Override
    public LocalizeValue getActionTitleValue() {
        return LocalizeValue.localizeTODO("Rename XML tag");
    }

    private static boolean isInplaceRenameAvailable(final Editor editor) {
        return editor.getSettings().isVariableInplaceRenameEnabled();
    }

    private static boolean isDeclarationOutOfProjectOrAbsent(@Nonnull final Project project, final DataContext context) {
        final PsiElement[] elements = BaseRefactoringAction.getPsiElementArray(context);
        return elements.length == 0 || elements.length == 1 && shouldBeRenamedInplace(project, elements);
    }

    private static boolean shouldBeRenamedInplace(Project project, PsiElement[] elements) {
        boolean inProject = PsiManager.getInstance(project).isInProject(elements[0]);
        if (inProject && elements[0] instanceof XmlTag) {
            XmlElementDescriptor descriptor = ((XmlTag)elements[0]).getDescriptor();
            return descriptor instanceof AnyXmlElementDescriptor;
        }
        return !inProject;
    }

    @Nullable
    private static Editor getEditor(@Nullable DataContext context) {
        return context == null ? null : context.getData(PlatformDataKeys.EDITOR);
    }

    @Nullable
    private static PsiElement getElement(@Nullable final DataContext context) {
        if (context != null) {
            final Editor editor = getEditor(context);
            if (editor != null) {
                final int offset = editor.getCaretModel().getOffset();
                final PsiFile file = context.getData(LangDataKeys.PSI_FILE);
                if (file instanceof XmlFile) {
                    return file.getViewProvider().findElementAt(offset);
                }
                if (file != null) {
                    final Language language = PsiUtilCore.getLanguageAtOffset(file, offset);
                    if (language != file.getLanguage()) {
                        final PsiFile psiAtOffset = file.getViewProvider().getPsi(language);
                        if (psiAtOffset instanceof XmlFile) {
                            return psiAtOffset.findElementAt(offset);
                        }
                    }
                }
            }
        }

        return null;
    }

    private void invoke(@Nullable final Editor editor, @Nonnull final PsiElement element, @Nullable final DataContext context) {
        if (!isRenaming(context)) {
            return;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed("refactoring.rename");

        if (isInplaceRenameAvailable(editor)) {
            XmlTagInplaceRenamer.rename(editor, (XmlTag)element.getParent());
        }
        else {
            XmlTagRenameDialog.renameXmlTag(editor, element, (XmlTag)element.getParent());
        }
    }

    @Override
    public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file, @Nullable final DataContext dataContext) {
        if (!isRenaming(dataContext)) {
            return;
        }

        final PsiElement element = getElement(dataContext);
        assert element != null;

        invoke(editor, element, dataContext);
    }

    @Override
    public void invoke(@Nonnull final Project project, @Nonnull final PsiElement[] elements, @Nullable final DataContext dataContext) {
        PsiElement element = elements.length == 1 ? elements[0] : null;
        if (element == null) {
            element = getElement(dataContext);
        }

        LOG.assertTrue(element != null);
        invoke(getEditor(dataContext), element, dataContext);
    }
}
