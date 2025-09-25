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
package consulo.xml.codeInsight.actions;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlProcessingInstruction;
import consulo.xml.psi.xml.XmlProlog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author ik
 * @since 2023-05-22
 */
@ActionImpl(id = "GenerateDTD")
public class GenerateDTDAction extends BaseCodeInsightAction {
    private static final Logger LOG = Logger.getInstance(GenerateDTDAction.class);

    public GenerateDTDAction() {
        super(XmlLocalize.actionGenerateDtdText(), LocalizeValue.empty());
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            @RequiredUIAccess
            public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
                XmlDocument document = findSuitableXmlDocument(file);
                if (document != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<!DOCTYPE ").append(document.getRootTag().getName()).append(" [\n");
                    sb.append(XmlUtil.generateDocumentDTD(document, true));
                    sb.append("]>\n");
                    try {
                        XmlProlog prolog = document.getProlog();
                        PsiElement childOfType = PsiTreeUtil.getChildOfType(prolog, XmlProcessingInstruction.class);
                        if (childOfType != null) {
                            String text = childOfType.getText();
                            sb.insert(0, text);
                            if (childOfType.getNextSibling() instanceof PsiWhiteSpace whiteSpace) {
                                sb.insert(text.length(), whiteSpace.getText());
                            }
                        }
                        XmlFile tempFile = (XmlFile) PsiFileFactory.getInstance(file.getProject())
                            .createFileFromText("dummy.xml", sb.toString());
                        prolog.replace(tempFile.getDocument().getProlog());
                    }
                    catch (IncorrectOperationException e) {
                        LOG.error(e);
                    }
                }
            }

            @Override
            public boolean startInWriteAction() {
                return true;
            }
        };
    }

    @Nullable
    private static XmlDocument findSuitableXmlDocument(@Nullable PsiFile psiFile) {
        if (psiFile instanceof XmlFile xmlFile) {
            XmlDocument document = xmlFile.getDocument();
            if (document != null && document.getRootTag() != null) {
                return document;
            }
        }
        return null;
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            Presentation presentation = event.getPresentation();
            presentation.setVisible(presentation.isEnabled());
        }
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return file.getLanguage() == XMLLanguage.INSTANCE && findSuitableXmlDocument(file) != null;
    }
}
