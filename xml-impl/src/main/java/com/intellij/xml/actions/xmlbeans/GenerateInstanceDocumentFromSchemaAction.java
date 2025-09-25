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

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.ApplicationExternalResourceManager;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.xml.XmlFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "Document2XSD")
public class GenerateInstanceDocumentFromSchemaAction extends AnAction {
    public GenerateInstanceDocumentFromSchemaAction() {
        super(XmlLocalize.actionDocumentToXsdText());
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

        GenerateInstanceDocumentFromSchemaDialog dialog = new GenerateInstanceDocumentFromSchemaDialog(project, file);
        dialog.setOkAction(() -> doAction(project, dialog));

        dialog.show();
    }

    @RequiredUIAccess
    private void doAction(Project project, GenerateInstanceDocumentFromSchemaDialog dialog) {
        FileDocumentManager.getInstance().saveAllDocuments();

        List<String> parameters = new LinkedList<>();

        String url = dialog.getUrl().getText();
        VirtualFile relativeFile =
            VirtualFileUtil.findRelativeFile(ApplicationExternalResourceManager.getInstance().getResourceLocation(url), null);
        PsiFile file = PsiManager.getInstance(project).findFile(relativeFile);
        if (!(file instanceof XmlFile)) {
            Messages.showErrorDialog(
                project,
                "This is not XmlFile" + (file == null ? "" : " (" + file.getFileType().getName() + ")"),
                XmlLocalize.error().get()
            );
            return;
        }

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

        if (!dialog.enableRestrictionCheck()) {
            parameters.add("-nopvr");
        }

        if (!dialog.enableUniquenessCheck()) {
            parameters.add("-noupa");
        }

        String pathToUse;

        try {
            File tempDir = FileUtil.createTempFile("xsd2inst", "");
            tempDir.delete();
            tempDir.mkdir();

            pathToUse = tempDir.getPath() + File.separatorChar + Xsd2InstanceUtils.processAndSaveAllSchemas(
                (XmlFile) file,
                new HashMap<>(),
                (schemaFileName, schemaContent) -> {
                    try {
                        String fullFileName = tempDir.getPath() + File.separatorChar + schemaFileName;
                        FileUtils.saveStreamContentAsFile(
                            fullFileName,
                            new ByteArrayInputStream(schemaContent)
                        );
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        }
        catch (IOException e) {
            return;
        }

        parameters.add(pathToUse);

        parameters.add("-name");
        parameters.add(dialog.getElementName());

        String xml;
        try {
            xml = Xsd2InstanceUtils.generate(ArrayUtil.toStringArray(parameters));
        }
        catch (IllegalArgumentException e) {
            Messages.showErrorDialog(project, ExceptionUtil.getMessage(e), XmlLocalize.error().get());
            return;
        }

        VirtualFile baseDirForCreatedInstanceDocument1 = relativeFileDir;
        String xmlFileName = baseDirForCreatedInstanceDocument1.getPath() + File.separatorChar + dialog.getOutputFileName();

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(xmlFileName);
            try {
                // the generated XML doesn't have any XML declaration -> utf-8
                fileOutputStream.write(xml.getBytes("utf-8"));
            }
            finally {
                fileOutputStream.close();
            }

            File xmlFile = new File(xmlFileName);
            VirtualFile virtualFile = Application.get().runWriteAction(
                (Supplier<VirtualFile>) () -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(xmlFile)
            );
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        }
        catch (IOException e) {
            Messages.showErrorDialog(
                project,
                "Could not save generated XML document: " + ExceptionUtil.getMessage(e),
                XmlLocalize.error().get()
            );
        }
    }

    static boolean isAcceptableFileForGenerateSchemaFromInstanceDocument(VirtualFile virtualFile) {
        return virtualFile != null && "xsd".equalsIgnoreCase(virtualFile.getExtension());
    }

    public static boolean isAcceptableFile(VirtualFile file) {
        return isAcceptableFileForGenerateSchemaFromInstanceDocument(file);
    }
}
