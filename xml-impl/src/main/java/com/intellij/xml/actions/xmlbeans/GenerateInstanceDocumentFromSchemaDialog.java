/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.xml.actions.xmlbeans;


import com.intellij.xml.XmlBundle;
import com.intellij.xml.XmlElementDescriptor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class GenerateInstanceDocumentFromSchemaDialog extends DialogWrapper {
    private JPanel panel;
    private TextFieldWithBrowseButton generateFromUrl;
    private JLabel status;
    private JLabel statusText;
    private JLabel generateFromUrlText;
    private JComboBox rootElementChooser;
    private JLabel rootElementChooserText;
    private JCheckBox enableRestrictionCheck;
    private JCheckBox enableUniqueCheck;
    private JTextField outputFileName;
    private JLabel outputFileNameText;
    private String previousUri;
    private Runnable myOkAction;
    private final Project myProject;

    public GenerateInstanceDocumentFromSchemaDialog(Project project, VirtualFile file) {
        super(project, true);
        myProject = project;

        UIUtils
            .configureBrowseButton(
                project,
                generateFromUrl,
                new String[]{"xsd"},
                XmlBundle.message("select.xsd.schema.dialog.title"),
                false
            );

        doInitFor(rootElementChooserText, rootElementChooser);
        doInitFor(generateFromUrlText, generateFromUrl.getTextField());
        doInitFor(outputFileNameText, outputFileName);
        generateFromUrl.setText(file.getPresentableUrl());
        updateFile();

        setTitle(XmlBundle.message("generate.instance.document.from.schema.dialog.title"));

        init();

        outputFileName.setText(file.getName() + ".xml");
    }

    public void doInitFor(JLabel textComponent, JComponent component) {
        textComponent.setLabelFor(component);

        if (component instanceof JTextField) {
            ((JTextField)component).getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    validateData();
                }

                public void removeUpdate(DocumentEvent e) {
                    validateData();
                }

                public void changedUpdate(DocumentEvent e) {
                    validateData();
                }
            });
        }
        else if (component instanceof JComboBox) {
            JComboBox jComboBox = ((JComboBox)component);

            jComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    validateData();
                }
            });

            ((JTextField)jComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    validateData();
                }

                public void removeUpdate(DocumentEvent e) {
                    validateData();
                }

                public void changedUpdate(DocumentEvent e) {
                    validateData();
                }
            });

            if (jComboBox.isEditable()) {
                jComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        validateData();
                    }
                });
            }
        }
    }

    private void validateData() {
        String msg = doValidateWithData();
        setOKActionEnabled(msg == null);
        status.setText(msg == null ? "" : msg);
        status.setForeground(JBColor.RED);
    }

    public static void configureComboBox(JComboBox combo, List<String> lastValues) {  // without -editor.selectAll- no focus
        combo.setModel(new DefaultComboBoxModel(ArrayUtil.toStringArray(lastValues)));
    }

    private void updateFile() {
        String uri = generateFromUrl.getText();
        boolean hasPrevious = (previousUri != null && previousUri.equals(uri));
        final PsiFile psifile = findFile(uri);
        List<String> myRootValues;

        if (psifile == null) {
            configureComboBox(rootElementChooser, Collections.<String>emptyList());
            return;
        }

        final XmlTag rootTag = getRootTag(psifile);

        if (rootTag == null) {
            configureComboBox(rootElementChooser, Collections.<String>emptyList());
            rootElementChooser.setSelectedIndex(-1);
            previousUri = uri;
            return;
        }

        myRootValues = Xsd2InstanceUtils.addVariantsFromRootTag(rootTag);

        Object selectedItem = rootElementChooser.getSelectedItem();
        configureComboBox(rootElementChooser, myRootValues);

        if (hasPrevious) {
            rootElementChooser.setSelectedItem(selectedItem);
        }
        else {
            rootElementChooser.setSelectedIndex(myRootValues.size() > 0 ? 0 : -1);
        }
        previousUri = uri;
    }

    @Nullable
    private static XmlTag getRootTag(PsiFile psifile) {
        XmlFile xmlFile = null;
        if (psifile instanceof XmlFile file) {
            xmlFile = file;
        }
        else if (psifile.getViewProvider() instanceof TemplateLanguageFileViewProvider viewProvider
            && viewProvider.getPsi(viewProvider.getTemplateDataLanguage()) instanceof XmlFile file) {
          xmlFile = file;
        }

      return xmlFile != null ? xmlFile.getDocument().getRootTag() : null;
    }

    @Nullable
    private PsiFile findFile(String uri) {
        final VirtualFile file = uri != null
            ? VirtualFileUtil.findRelativeFile(ExternalResourceManager.getInstance().getResourceLocation(uri), null) : null;
        return file != null ? PsiManager.getInstance(myProject).findFile(file) : null;
    }

    public String getOutputFileName() {
        return outputFileName.getText();
    }

    public Boolean areCurrentParametersStillValid() {
        updateFile();
        return rootElementChooser.getSelectedItem() != null;
    }

    @Nullable
    protected String doValidateWithData() {
        String rootElementName = getElementName();
        if (rootElementName == null || rootElementName.length() == 0) {
            return XmlBundle.message("schema2.instance.no.valid.root.element.name.validation.error");
        }

        final PsiFile psiFile = findFile(getUrl().getText());
        if (psiFile instanceof XmlFile) {
            final XmlTag tag = getRootTag(psiFile);
            if (tag != null) {
                final XmlElementDescriptor descriptor = Xsd2InstanceUtils.getDescriptor(tag, rootElementName);

                if (descriptor == null) {
                    return XmlBundle.message("schema2.instance.no.valid.root.element.name.validation.error");
                }
            }
        }

        final String fileName = getOutputFileName();
        if (fileName == null || fileName.length() == 0) {
            return XmlBundle.message("schema2.instance.output.file.name.is.empty.validation.problem");
        }
        return null;

    }

    protected static boolean isAcceptableFile(VirtualFile virtualFile) {
        return GenerateInstanceDocumentFromSchemaAction.isAcceptableFileForGenerateSchemaFromInstanceDocument(virtualFile);
    }


    protected TextFieldWithBrowseButton getUrl() {
        return generateFromUrl;
    }

    protected JLabel getUrlText() {
        return generateFromUrlText;
    }

    protected JLabel getStatusTextField() {
        return statusText;
    }

    protected JLabel getStatusField() {
        return status;
    }

    protected JComponent createCenterPanel() {
        return panel;
    }

    boolean enableUniquenessCheck() {
        return enableUniqueCheck.isSelected();
    }

    boolean enableRestrictionCheck() {
        return enableRestrictionCheck.isSelected();
    }

    String getElementName() {
        return (String)rootElementChooser.getSelectedItem();
    }

    public void setOkAction(Runnable runnable) {
        myOkAction = runnable;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (myOkAction != null) {
            myOkAction.run();
        }
    }

    @Nonnull
    protected String getHelpId() {
        return "webservices.GenerateInstanceDocumentFromSchema";
    }
}
