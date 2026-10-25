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

import consulo.application.util.SystemInfo;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.project.Project;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.localize.XmlLocalize;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
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
        wsdlUrl.getButton().setToolTipText(XmlLocalize.browseButtonTooltip().get());
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

                String basePath = myProject.getBasePath();
                Path initialPath = basePath == null ? null : Path.of(basePath);
                String selectedItem = wsdlUrl.getTextField().getText();
                if (selectedItem != null && selectedItem.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
                    String location = ExternalResourceManager.getInstance().getResourceLocation(VirtualFileUtil.fixURLforIDEA(selectedItem));
                    Path pathByUrl = NioFiles.toPath(FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(location)));
                    if (pathByUrl != null) {
                        initialPath = pathByUrl;
                    }
                }

                FileChooserFactory.getInstance()
                    .newBuilder(fileChooserDescriptor)
                    .project(myProject)
                    .toSelect(initialPath)
                    .showMultipleAsVirtualFiles()
                    .whenComplete((virtualFiles, error) -> {
                        if (error == null && virtualFiles.size() == 1) {
                            wsdlUrl.setText(fixIDEAUrl(virtualFiles.get(0).getUrl()));
                        }
                    });
            }
        });
    }

    public static String fixIDEAUrl(String url) {
        return SystemInfo.isWindows ? VirtualFileUtil.fixIDEAUrl(url) : url;
    }
}
