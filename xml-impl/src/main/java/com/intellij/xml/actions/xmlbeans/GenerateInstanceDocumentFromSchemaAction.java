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

import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.psi.xml.XmlFile;
import com.intellij.xml.XmlBundle;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class GenerateInstanceDocumentFromSchemaAction extends AnAction {
  @Override
  public void update(AnActionEvent e) {
    final VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
    final boolean enabled = isAcceptableFile(file);
    e.getPresentation().setEnabled(enabled);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(enabled);
    }
  }

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);

    final GenerateInstanceDocumentFromSchemaDialog dialog = new GenerateInstanceDocumentFromSchemaDialog(project, file);
    dialog.setOkAction(new Runnable() {
      public void run() {
        doAction(project, dialog);
      }
    });

    dialog.show();
  }

  private void doAction(final Project project, final GenerateInstanceDocumentFromSchemaDialog dialog) {
    FileDocumentManager.getInstance().saveAllDocuments();

    @NonNls List<String> parameters = new LinkedList<String>();

    final String url = dialog.getUrl().getText();
    final VirtualFile relativeFile = VfsUtil.findRelativeFile(ExternalResourceManager.getInstance().getResourceLocation(url), null);
    final PsiFile file = PsiManager.getInstance(project).findFile(relativeFile);
    if (! (file instanceof XmlFile)) {
      Messages.showErrorDialog(project, "This is not XmlFile" + file == null ? "" : " (" + file.getFileType().getName() + ")", XmlBundle.message("error"));
      return;
    }

    VirtualFile relativeFileDir;
    if (relativeFile == null) {
      Messages.showErrorDialog(project, XmlBundle.message("file.doesnt.exist", url), XmlBundle.message("error"));
      return;
    } else {
      relativeFileDir = relativeFile.getParent();
    }
    if (relativeFileDir == null) {
      Messages.showErrorDialog(project, XmlBundle.message("file.doesnt.exist", url), XmlBundle.message("error"));
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
      final File tempDir = FileUtil.createTempFile("xsd2inst", "");
      tempDir.delete();
      tempDir.mkdir();

      pathToUse = tempDir.getPath() + File.separatorChar + Xsd2InstanceUtils.processAndSaveAllSchemas(
        (XmlFile) file,
        new HashMap<String, String>(),
        new Xsd2InstanceUtils.SchemaReferenceProcessor() {
          public void processSchema(String schemaFileName, byte[] schemaContent) {
            try {
              final String fullFileName = tempDir.getPath() + File.separatorChar + schemaFileName;
              FileUtils.saveStreamContentAsFile(
                fullFileName,
                new ByteArrayInputStream(schemaContent)
              );
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
      );
    } catch (IOException e) {
      return;
    }

    parameters.add(pathToUse);

    parameters.add("-name");
    parameters.add(dialog.getElementName());

    String xml;
    try {
      xml = Xsd2InstanceUtils.generate(ArrayUtil.toStringArray(parameters));
    } catch (IllegalArgumentException e) {
      Messages.showErrorDialog(project, ExceptionUtil.getMessage(e), XmlBundle.message("error"));
      return;
    }



    final VirtualFile baseDirForCreatedInstanceDocument1 = relativeFileDir;
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

      final File xmlFile = new File(xmlFileName);
      VirtualFile virtualFile = ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
        @Nullable
        public VirtualFile compute() {
          return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(xmlFile);
        }
      });
      FileEditorManager.getInstance(project).openFile(virtualFile, true);
    }
    catch (IOException e) {
      Messages.showErrorDialog(project, "Could not save generated XML document: " + ExceptionUtil.getMessage(e), XmlBundle.message("error"));
    }
  }

  static boolean isAcceptableFileForGenerateSchemaFromInstanceDocument(VirtualFile virtualFile) {
    return virtualFile != null && "xsd".equalsIgnoreCase(virtualFile.getExtension());
  }

  public static boolean isAcceptableFile(VirtualFile file) {
    return isAcceptableFileForGenerateSchemaFromInstanceDocument(file);
  }
}
