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
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlProcessingInstruction;
import consulo.xml.psi.xml.XmlProlog;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 22.05.2003
 * Time: 13:46:54
 */
public class GenerateDTDAction extends BaseCodeInsightAction {
    private static final Logger LOG = Logger.getInstance(GenerateDTDAction.class);

    @Nonnull
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
                final XmlDocument document = findSuitableXmlDocument(file);
                if (document != null) {
                    final @NonNls StringBuffer buffer = new StringBuffer();
                    buffer.append("<!DOCTYPE " + document.getRootTag().getName() + " [\n");
                    buffer.append(XmlUtil.generateDocumentDTD(document, true));
                    buffer.append("]>\n");
                    XmlFile tempFile;
                    try {
                        final XmlProlog prolog = document.getProlog();
                        final PsiElement childOfType = PsiTreeUtil.getChildOfType(prolog, XmlProcessingInstruction.class);
                        if (childOfType != null) {
                            final String text = childOfType.getText();
                            buffer.insert(0, text);
                            final PsiElement nextSibling = childOfType.getNextSibling();
                            if (nextSibling instanceof PsiWhiteSpace) {
                                buffer.insert(text.length(), nextSibling.getText());
                            }
                        }
                        tempFile =
                            (XmlFile)PsiFileFactory.getInstance(file.getProject()).createFileFromText("dummy.xml", buffer.toString());
                        prolog.replace(tempFile.getDocument().getProlog());
                    }
                    catch (IncorrectOperationException e) {
                        LOG.error(e);
                    }
                }
            }

            public boolean startInWriteAction() {
                return true;
            }
        };
    }

    @Nullable
    private static XmlDocument findSuitableXmlDocument(@Nullable PsiFile psiFile) {
        if (psiFile instanceof XmlFile) {
            final XmlDocument document = ((XmlFile)psiFile).getDocument();
            if (document != null && document.getRootTag() != null) {
                return document;
            }
        }
        return null;
    }

    public void update(AnActionEvent event) {
        super.update(event);
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            Presentation presentation = event.getPresentation();
            presentation.setVisible(presentation.isEnabled());
        }
    }

    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return file.getLanguage() == XMLLanguage.INSTANCE && findSuitableXmlDocument(file) != null;
    }
}
