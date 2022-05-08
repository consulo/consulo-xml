/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.config;

import consulo.xml.ide.highlighter.XmlFileType;
import consulo.configurable.ConfigurationException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.HectorComponentPanel;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.intellij.plugins.relaxNG.compact.RncFileType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 25.11.2007
 */
class NoNamespaceConfigPanel extends HectorComponentPanel {
  private final NoNamespaceConfig myConfig;
  private final PsiFile myFile;
  private final String myMapping;

  private TextFieldWithBrowseButton mySchemaFile;
  private JPanel myRoot;
  private boolean myDialogOpen;

  NoNamespaceConfigPanel(NoNamespaceConfig noNamespaceConfig, PsiFile file) {
    myConfig = noNamespaceConfig;
    myFile = file;
    myMapping = myConfig.getMapping(file);

    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        final boolean b = super.isFileSelectable(file);
        if (b) {
          final FileType type = file.getFileType();
          if (type != XmlFileType.INSTANCE) {
            return type == RncFileType.getInstance();
          }
        }
        return b;
      }
    };

    final Project project = file.getProject();
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile != null) {
      final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
      descriptor.putUserData(LangDataKeys.MODULE_CONTEXT, module);
    }

    // that's kind of a hack that ensures the Hector Panel stays open when selecting a file. otherwise it will close
    // as soon as a file selected.
    final ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> actionListener =
            new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>("Select Schema", "Select a RELAX-NG file to associate with the document",
                    mySchemaFile, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
              @Override
              public void actionPerformed(ActionEvent e) {
                myDialogOpen = true;
                try {
                  super.actionPerformed(e);
                } finally {
                  myDialogOpen = false;
                }
              }
            };

    mySchemaFile.addActionListener(actionListener);
  }

  @Override
  public boolean canClose() {
    return super.canClose() && !myDialogOpen;
  }

  @Override
  public JComponent createComponent() {
    return myRoot;
  }

  @Override
  public boolean isModified() {
    final String s = mySchemaFile.getText();
    final String m = myMapping != null ? myMapping : "";
    return !s.equals(m);
  }

  @Override
  public void apply() throws ConfigurationException {
    final String s = getMapping();
    if (s != null) {
      myConfig.setMapping(myFile, s);
    } else {
      myConfig.setMapping(myFile, null);
    }
  }

  private String getMapping() {
    final String s = mySchemaFile.getText().trim();
    return s.length() > 0 ? VfsUtil.pathToUrl(s.replace(File.separatorChar, '/')) : null;
  }

  @Override
  public void reset() {
    mySchemaFile.setText(myMapping != null ? VfsUtil.urlToPath(myMapping).replace('/', File.separatorChar) : "");
  }

  @Override
  public void disposeUIResources() {
    // doesn't help - updating the validation needs a hard modification
//    DaemonCodeAnalyzer.getInstance(myFile.getProject()).restart();
  }
}
