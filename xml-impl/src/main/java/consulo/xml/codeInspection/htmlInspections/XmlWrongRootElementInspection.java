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

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Result;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
@ExtensionImpl
public class XmlWrongRootElementInspection extends HtmlLocalInspectionTool {
    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlInspectionGroupNames.XML_INSPECTIONS;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.xmlInspectionWrongRootElement();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "XmlWrongRootElement";
    }

    @Override
    @Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    protected void checkTag(@Nonnull XmlTag tag, @Nonnull ProblemsHolder holder, boolean isOnTheFly, Object state) {
        if (!(tag.getParent() instanceof XmlTag)) {
            if (!(tag.getContainingFile() instanceof XmlFile xmlFile)) {
                return;
            }

            XmlDocument document = xmlFile.getDocument();
            if (document == null) {
                return;
            }

            XmlProlog prolog = document.getProlog();
            if (prolog == null || XmlHighlightVisitor.skipValidation(prolog)) {
                return;
            }

            XmlDoctype doctype = prolog.getDoctype();

            if (doctype == null) {
                return;
            }

            XmlElement nameElement = doctype.getNameElement();

            if (nameElement == null) {
                return;
            }

            String name = tag.getName();
            String text = nameElement.getText();
            if (tag instanceof HtmlTag) {
                name = name.toLowerCase();
                text = text.toLowerCase();
            }

            if (!name.equals(text)) {
                name = XmlUtil.findLocalNameByQualifiedName(name);
                if (!name.equals(text)) {
                    if (tag instanceof HtmlTag) {
                        return; // it is legal to have html / head / body omitted
                    }
                    LocalQuickFix localQuickFix = new MyLocalQuickFix(doctype.getNameElement().getText());

                    holder.newProblem(XmlErrorLocalize.wrongRootElement())
                        .range(XmlChildRole.START_TAG_NAME_FINDER.findChild(tag.getNode()).getPsi())
                        .withFix(localQuickFix)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create();

                    ASTNode astNode = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(tag.getNode());
                    if (astNode != null) {
                        holder.newProblem(XmlErrorLocalize.wrongRootElement())
                            .range(astNode.getPsi())
                            .withFix(localQuickFix)
                            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                            .create();
                    }
                }
            }
        }
    }

    private static class MyLocalQuickFix implements LocalQuickFix {
        private final String myText;

        public MyLocalQuickFix(String text) {
            myText = text;
        }

        @Nonnull
        @Override
        public LocalizeValue getName() {
            return XmlLocalize.changeRootElementTo(myText);
        }

        @Override
        @RequiredUIAccess
        public void applyFix(@Nonnull final Project project, @Nonnull ProblemDescriptor descriptor) {
            final XmlTag myTag = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), XmlTag.class);

            if (!FileModificationService.getInstance().prepareFileForWrite(myTag.getContainingFile())) {
                return;
            }

            new WriteCommandAction(project) {
                @Override
                protected void run(Result result) throws Throwable {
                    myTag.setName(myText);
                }
            }.execute();
        }
    }
}
