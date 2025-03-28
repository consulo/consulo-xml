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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.XmlNamespaceHelper;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.hint.QuestionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.JBPopup;
import consulo.xml.psi.xml.XmlFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class ImportNSAction implements QuestionAction {
    private final Set<String> myNamespaces;
    private final XmlFile myFile;
    private final PsiElement myElement;
    private final Editor myEditor;
    private final String myTitle;

    public ImportNSAction(Set<String> namespaces, XmlFile file, @Nonnull PsiElement element, Editor editor, String title) {
        myNamespaces = namespaces;
        myFile = file;
        myElement = element;
        myEditor = editor;
        myTitle = title;
    }

    @Override
    public boolean execute() {
        Object[] objects = myNamespaces.toArray();
        Arrays.sort(objects);
        JList list = new JBList(objects);
        list.setCellRenderer(XmlNSRenderer.INSTANCE);
        list.setSelectedIndex(0);
        int offset = myElement.getTextOffset();
        RangeMarker marker = myEditor.getDocument().createRangeMarker(offset, offset);
        Runnable runnable = () -> {
            String namespace = (String)list.getSelectedValue();
            if (namespace != null) {
                Project project = myFile.getProject();
                new WriteCommandAction.Simple(project, myFile) {
                    protected void run() throws Throwable {
                        XmlNamespaceHelper extension = XmlNamespaceHelper.getHelper(myFile);
                        String prefix = extension.getNamespacePrefix(myElement);
                        extension.insertNamespaceDeclaration(
                            myFile,
                            myEditor,
                            Collections.singleton(namespace),
                            prefix,
                            s -> {
                                PsiDocumentManager.getInstance(myFile.getProject())
                                    .doPostponedOperationsAndUnblockDocument(myEditor.getDocument());
                                PsiElement element = myFile.findElementAt(marker.getStartOffset());
                                if (element != null) {
                                    extension.qualifyWithPrefix(s, element, myEditor.getDocument());
                                }
                            }
                        );
                    }
                }.execute();
            }
        };
        if (list.getModel().getSize() == 1) {
            runnable.run();
        }
        else {
            JBPopup popup = new PopupChooserBuilder(list)
                .setTitle(myTitle)
                .setItemChoosenCallback(runnable)
                .createPopup();

            myEditor.showPopupInBestPositionFor(popup);
        }

        return true;
    }
}
