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
package com.intellij.xml.actions.xmlbeans;

import com.intellij.xml.XmlBundle;
import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.ApplicationExternalResourceManager;
import consulo.xml.localize.XmlLocalize;
import org.apache.xmlbeans.impl.inst2xsd.Inst2Xsd;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "XSD2Document")
public class GenerateSchemaFromInstanceDocumentAction extends AnAction {
    private static final Map<String, String> DESIGN_TYPES = new HashMap<>();
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();

    static {
        DESIGN_TYPES.put(GenerateSchemaFromInstanceDocumentDialog.LOCAL_ELEMENTS_GLOBAL_COMPLEX_TYPES, "vb");
        DESIGN_TYPES.put(GenerateSchemaFromInstanceDocumentDialog.LOCAL_ELEMENTS_TYPES, "ss");
        DESIGN_TYPES.put(GenerateSchemaFromInstanceDocumentDialog.GLOBAL_ELEMENTS_LOCAL_TYPES, "rd");
        CONTENT_TYPES.put(GenerateSchemaFromInstanceDocumentDialog.SMART_TYPE, "smart");
        CONTENT_TYPES.put(GenerateSchemaFromInstanceDocumentDialog.STRING_TYPE, "string");
    }

    public GenerateSchemaFromInstanceDocumentAction() {
        super(XmlLocalize.actionXsdToDocumentText());
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        boolean enabled = isAcceptableFile(file);
        e.getPresentation().setEnabled(enabled);
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(enabled);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile file = e.getRequiredData(VirtualFile.KEY);

        GenerateSchemaFromInstanceDocumentDialog dialog = new GenerateSchemaFromInstanceDocumentDialog(project, file);
        dialog.setOkAction(() -> doAction(project, dialog));

        dialog.show();
    }

    @RequiredUIAccess
    private static void doAction(Project project, GenerateSchemaFromInstanceDocumentDialog dialog) {
        FileDocumentManager.getInstance().saveAllDocuments();

        String url = dialog.getUrl().getText();
        VirtualFile relativeFile =
            VirtualFileUtil.findRelativeFile(ApplicationExternalResourceManager.getInstance().getResourceLocation(url), null);
        VirtualFile relativeFileDir;
        if (relativeFile == null) {
            Messages.showErrorDialog(project, XmlLocalize.fileDoesntExist(url).get(), XmlLocalize.error().get());
            return;
        }
        else {
            relativeFileDir = relativeFile.getParent();
        }
        if (relativeFileDir == null) {
            Messages.showErrorDialog(project, XmlLocalize.fileDoesntExist(url).get(), XmlLocalize.error().get());
            return;
        }

        List<String> parameters = new LinkedList<>();
        parameters.add("-design");
        parameters.add(DESIGN_TYPES.get(dialog.getDesignType()));

        parameters.add("-simple-content-types");
        parameters.add(CONTENT_TYPES.get(dialog.getSimpleContentType()));

        parameters.add("-enumerations");
        String enumLimit = dialog.getEnumerationsLimit();
        parameters.add("0".equals(enumLimit) ? "never" : enumLimit);

        parameters.add("-outDir");
        String dirPath = relativeFileDir.getPath();
        parameters.add(dirPath);

        File expectedSchemaFile = new File(dirPath + File.separator + relativeFile.getName() + "0.xsd");
        if (expectedSchemaFile.exists()) {
            if (!expectedSchemaFile.delete()) {
                Messages.showErrorDialog(
                    project,
                    XmlBundle.message("cant.delete.file", expectedSchemaFile.getPath()),
                    XmlLocalize.error().get()
                );
                return;
            }
        }

        parameters.add("-outPrefix");
        parameters.add(relativeFile.getName());

        parameters.add(url);
        File xsd = new File(dirPath + File.separator + dialog.getTargetSchemaName());
        VirtualFile xsdFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(xsd);
        if (xsdFile != null) {
            Application.get().runWriteAction(() -> {
                try {
                    xsdFile.delete(null);
                }
                catch (IOException e) {//
                }
            });
        }

        Inst2Xsd.main(ArrayUtil.toStringArray(parameters));
        if (expectedSchemaFile.exists()) {
            boolean renamed = expectedSchemaFile.renameTo(xsd);
            if (!renamed) {
                Messages.showErrorDialog(
                    project,
                    XmlBundle.message("cant.rename.file", expectedSchemaFile.getPath(), xsd.getPath()),
                    XmlLocalize.error().get()
                );
            }
        }

        VirtualFile xsdVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(xsd);
        if (xsdVFile != null) {
            FileEditorManager.getInstance(project).openFile(xsdVFile, true);
        }
        else {
            Messages.showErrorDialog(
                project,
                XmlLocalize.xml2xsdGeneratorErrorMessage().get(),
                XmlLocalize.xml2xsdGeneratorError().get()
            );
        }
    }

    public static boolean isAcceptableFile(VirtualFile file) {
        return file != null && "xml".equalsIgnoreCase(file.getExtension());
    }
}
