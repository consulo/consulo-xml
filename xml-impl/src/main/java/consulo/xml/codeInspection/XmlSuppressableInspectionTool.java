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
package consulo.xml.codeInspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.BatchSuppressableTool;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class XmlSuppressableInspectionTool extends LocalInspectionTool implements BatchSuppressableTool {
    static final String ALL = "ALL";

    @Nonnull
    @Override
    public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
        return getSuppressFixes(getID());
    }

    public static SuppressQuickFix[] getSuppressFixes(String shortName) {
        String id = HighlightDisplayKey.find(shortName).getID();
        return new SuppressQuickFix[]{
            new SuppressTagStatic(id),
            new SuppressForFile(id),
            new SuppressAllForFile()
        };
    }

    @Override
    public boolean isSuppressedFor(@Nonnull PsiElement element) {
        return XmlSuppressionProvider.isSuppressed(element, getID());
    }

    public class SuppressTag extends SuppressTagStatic {
        public SuppressTag() {
            super(getID());
        }
    }

    public static class SuppressTagStatic implements SuppressQuickFix {
        private final String id;

        public SuppressTagStatic(@Nonnull String id) {
            this.id = id;
        }

        @Nonnull
        @Override
        public LocalizeValue getName() {
            return InspectionLocalize.xmlSuppressableForTagTitle();
        }

        @Override
        @RequiredReadAction
        public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
            return context.isValid();
        }

        @Override
        @RequiredUIAccess
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (PsiTreeUtil.getParentOfType(element, XmlTag.class) == null) {
                return;
            }
            XmlSuppressionProvider.getProvider(element.getContainingFile()).suppressForTag(element, id);
        }
    }

    public static class SuppressForFile implements SuppressQuickFix {
        private final String myInspectionId;

        public SuppressForFile(@Nonnull String inspectionId) {
            myInspectionId = inspectionId;
        }

        @Nonnull
        @Override
        public LocalizeValue getName() {
            return InspectionLocalize.xmlSuppressableForFileTitle();
        }

        @Override
        @RequiredReadAction
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null || !element.isValid() || !(element.getContainingFile() instanceof XmlFile)) {
                return;
            }
            XmlSuppressionProvider.getProvider(element.getContainingFile()).suppressForFile(element, myInspectionId);
        }

        @Override
        @RequiredReadAction
        public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
            return context.isValid();
        }
    }

    public static class SuppressAllForFile extends SuppressForFile {
        public SuppressAllForFile() {
            super(ALL);
        }

        @Nonnull
        @Override
        public LocalizeValue getName() {
            return InspectionLocalize.xmlSuppressableAllForFileTitle();
        }
    }
}
