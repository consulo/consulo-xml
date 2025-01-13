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
import consulo.application.util.SystemInfo;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.project.Project;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.ExternalResourceManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class UIUtils {
    private UIUtils() {
    }

    public static void configureBrowseButton(
        final Project myProject,
        final TextFieldWithBrowseButton wsdlUrl,
        final String[] _extensions,
        final String selectFileDialogTitle,
        final boolean multipleFileSelection
    ) {
        wsdlUrl.getButton().setToolTipText(XmlBundle.message("browse.button.tooltip"));
        wsdlUrl.getButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                final FileChooserDescriptor fileChooserDescriptor =
                    new FileChooserDescriptor(true, false, false, false, false, multipleFileSelection) {
                        private final List<String> extensions = Arrays.asList(_extensions);

                        public boolean isFileSelectable(VirtualFile virtualFile) {
                            return extensions.contains(virtualFile.getExtension());
                        }

                        @Override
                        public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                            return super.isFileVisible(file, showHiddenFiles) && (file.isDirectory() || isFileSelectable(file));
                        }
                    };

                fileChooserDescriptor.setTitle(selectFileDialogTitle);

                VirtualFile initialFile = myProject.getBaseDir();
                String selectedItem = wsdlUrl.getTextField().getText();
                if (selectedItem != null && selectedItem.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
                    VirtualFile fileByPath = VirtualFileUtil.findRelativeFile(ExternalResourceManager.getInstance()
                        .getResourceLocation(VirtualFileUtil.fixURLforIDEA(selectedItem)), null);
                    if (fileByPath != null) initialFile = fileByPath;
                }

                final VirtualFile[] virtualFiles = IdeaFileChooser.chooseFiles(fileChooserDescriptor, myProject, initialFile);
                if (virtualFiles.length == 1) {
                    String url = fixIDEAUrl(virtualFiles[0].getUrl());
                    wsdlUrl.setText(url);
                }
            }
        });
    }

    public static String fixIDEAUrl(String url) {
        return SystemInfo.isWindows ? VirtualFileUtil.fixIDEAUrl(url) : url;
    }
}
