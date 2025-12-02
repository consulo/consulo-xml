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
package consulo.xml.util.xml.ui;

import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementsProblemsHolder;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class EditorTextFieldControl<T extends JComponent> extends BaseModifiableControl<T, String> {
    private static final JTextField J_TEXT_FIELD = new JTextField() {
        @Override
        public void addNotify() {
            throw new UnsupportedOperationException("Shouldn't be shown");
        }

        @Override
        public void setVisible(boolean aFlag) {
            throw new UnsupportedOperationException("Shouldn't be shown");
        }
    };
    private final boolean myCommitOnEveryChange;
    private final DocumentListener myListener = new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
            setModified();
            if (myCommitOnEveryChange) {
                commit();
            }
        }
    };

    protected EditorTextFieldControl(DomWrapper<String> domWrapper, boolean commitOnEveryChange) {
        super(domWrapper);
        myCommitOnEveryChange = commitOnEveryChange;
    }

    protected EditorTextFieldControl(DomWrapper<String> domWrapper) {
        this(domWrapper, false);
    }

    protected abstract EditorTextField getEditorTextField(@Nonnull T component);

    @Override
    protected void doReset() {
        EditorTextField textField = getEditorTextField(getComponent());
        textField.getDocument().removeDocumentListener(myListener);
        super.doReset();
        textField.getDocument().addDocumentListener(myListener);
    }

    @Override
    protected JComponent getComponentToListenFocusLost(T component) {
        return getEditorTextField(getComponent());
    }

    @Override
    protected JComponent getHighlightedComponent(T component) {
        return J_TEXT_FIELD;
    }

    @Override
    protected T createMainComponent(T boundedComponent) {
        Project project = getProject();
        boundedComponent = createMainComponent(boundedComponent, project);

        EditorTextField editorTextField = getEditorTextField(boundedComponent);
        editorTextField.setSupplementary(true);
        editorTextField.getDocument().addDocumentListener(myListener);
        return boundedComponent;
    }

    protected abstract T createMainComponent(T boundedComponent, Project project);

    @Nonnull
    @Override
    public String getValue() {
        return getEditorTextField(getComponent()).getText();
    }

    @Override
    @RequiredUIAccess
    public void setValue(String value) {
        CommandProcessor.getInstance().runUndoTransparentAction(() -> WriteAction.run(() -> {
            T component = getComponent();
            Document document = getEditorTextField(component).getDocument();
            document.replaceString(0, document.getTextLength(), value == null ? "" : value);
        }));
    }

    @Override
    protected void updateComponent() {
        DomElement domElement = getDomElement();
        if (domElement == null || !domElement.isValid()) {
            return;
        }

        EditorTextField textField = getEditorTextField(getComponent());
        Project project = getProject();
        project.getApplication().invokeLater(() -> {
            if (!project.isOpen()) {
                return;
            }
            if (!getDomWrapper().isValid()) {
                return;
            }

            DomElement domElement1 = getDomElement();
            if (domElement1 == null || !domElement1.isValid()) {
                return;
            }

            DomElementAnnotationsManager manager = DomElementAnnotationsManager.getInstance(project);
            DomElementsProblemsHolder holder = manager.getCachedProblemHolder(domElement1);
            List<DomElementProblemDescriptor> errorProblems = holder.getProblems(domElement1);
            List<DomElementProblemDescriptor> warningProblems =
                new ArrayList<>(holder.getProblems(domElement1, true, HighlightSeverity.WARNING));
            warningProblems.removeAll(errorProblems);

            Color background = getDefaultBackground();
            if (errorProblems.size() > 0 && textField.getText().trim().length() == 0) {
                background = getErrorBackground();
            }
            else if (warningProblems.size() > 0) {
                background = getWarningBackground();
            }

            Editor editor = textField.getEditor();
            if (editor != null) {
                MarkupModel markupModel = editor.getMarkupModel();
                markupModel.removeAllHighlighters();
                if (!errorProblems.isEmpty() && editor.getDocument().getLineCount() > 0) {
                    TextAttributes attributes = TextAttributesUtil.toTextAttributes(SimpleTextAttributes.ERROR_ATTRIBUTES);
                    attributes.setEffectType(EffectType.WAVE_UNDERSCORE);
                    attributes.setEffectColor(attributes.getForegroundColor());
                    markupModel.addLineHighlighter(0, 0, attributes);
                    editor.getContentComponent().setToolTipText(errorProblems.get(0).getDescriptionTemplate().get());
                }
            }

            textField.setBackground(background);
        });
    }

    @Override
    public boolean canNavigate(DomElement element) {
        return getDomElement().equals(element);
    }

    @Override
    public void navigate(DomElement element) {
        EditorTextField field = getEditorTextField(getComponent());
        SwingUtilities.invokeLater(() -> {
            field.requestFocus();
            field.selectAll();
        });
    }
}
