/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.XmlRecursiveElementVisitor;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl
public class XmlHighlightVisitorBasedInspection extends GlobalSimpleInspectionTool {
    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public void checkFile(
        @Nonnull final PsiFile file,
        @Nonnull final InspectionManager manager,
        @Nonnull ProblemsHolder problemsHolder,
        @Nonnull final GlobalInspectionContext globalContext,
        @Nonnull final ProblemDescriptionsProcessor problemDescriptionsProcessor,
        @Nonnull Object state
    ) {
        HighlightInfoHolder myHolder = new HighlightInfoHolder(file, List.of()) {
            @Override
            public boolean add(@Nullable HighlightInfo info) {
                if (info != null) {
                    GlobalInspectionUtil.createProblem(
                        file,
                        info,
                        new TextRange(info.getStartOffset(), info.getEndOffset()),
                        null,
                        manager,
                        problemDescriptionsProcessor,
                        globalContext
                    );
                }
                return true;
            }
        };
        final XmlHighlightVisitor highlightVisitor = new XmlHighlightVisitor();
        highlightVisitor.analyze(file, true, myHolder, new Runnable() {
            @Override
            public void run() {
                file.accept(new XmlRecursiveElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        highlightVisitor.visit(element);
                        super.visitElement(element);
                    }
                });
            }
        });

    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nls
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return "General";
    }

    @Nls
    @Nonnull
    @Override
    public String getDisplayName() {
        return "XML highlighting";
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "XmlHighlighting";
    }
}
