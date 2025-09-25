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
package org.intellij.plugins.relaxNG.convert;

import java.io.File;

import com.intellij.xml.actions.XmlActionsGroup;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.xml.codeInsight.actions.GenerateDTDAction;
import consulo.xml.relaxNG.localize.RelaxNGLocalize;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.validation.RngValidateHandler;
import consulo.xml.ide.highlighter.DTDFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

/**
 * @author sweinreuter
 * @since 2007-11-16
 */
@ActionImpl(
    id = "ConvertSchemaAction",
    parents = @ActionParentRef(
        value = @ActionRef(type = XmlActionsGroup.class),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(type = GenerateDTDAction.class)
    )
)
public class ConvertSchemaAction extends AnAction {
    public ConvertSchemaAction() {
        super(RelaxNGLocalize.actionConvertSchemaText(), RelaxNGLocalize.actionConvertSchemaDescription());
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        Project project = e.getData(Project.KEY);
        if (files != null && project != null) {
            SchemaType type = getInputType(project, files);
            e.getPresentation().setEnabled(type != null);
            e.getPresentation().setTextValue(
                type == SchemaType.XML
                    ? RelaxNGLocalize.actionConvertSchemaFromXmlFilesText(files.length)
                    : RelaxNGLocalize.actionConvertSchemaText()
            );
        }
        else {
            e.getPresentation().setEnabled(false);
        }
    }

    @RequiredReadAction
    private static SchemaType getInputType(Project project, VirtualFile... files) {
        if (files.length == 0) {
            return null;
        }

        VirtualFile file = files[0];
        FileType type = file.getFileType();
        if (type == XmlFileType.INSTANCE) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof XmlFile xmlFile) {
                XmlDocument document = xmlFile.getDocument();
                if (document != null && document.getRootTag() != null) {
                    XmlTag rootTag = document.getRootTag();
                    assert rootTag != null;
                    String uri = rootTag.getNamespace();
                    if (ApplicationLoader.RNG_NAMESPACE.equals(uri) && files.length == 1) {
                        return SchemaType.RNG;
                    }
                }
            }
            if (files.length > 1) {
                for (VirtualFile virtualFile : files) {
                    if (virtualFile.getFileType() != XmlFileType.INSTANCE || getInputType(project, virtualFile) != null) {
                        return null;
                    }
                }
            }
            return SchemaType.XML;
        }
        else if (type == DTDFileType.INSTANCE && files.length == 1) {
            return SchemaType.DTD;
        }
        else if (type == RncFileType.getInstance() && files.length == 1) {
            return SchemaType.RNC;
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        VirtualFile file = e.getData(VirtualFile.KEY);
        Project project = e.getData(Project.KEY);
        if (file != null && project != null) {
            VirtualFile[] files = e.getRequiredData(VirtualFile.KEY_OF_ARRAY);

            SchemaType type = getInputType(project, files);
            ConvertSchemaDialog dialog = new ConvertSchemaDialog(project, type, file);
            if (!dialog.showAndGet()) {
                return;
            }

            RngValidateHandler.saveFiles(files);

            ConvertSchemaSettings settings = dialog.getSettings();
            IdeaErrorHandler errorHandler = new IdeaErrorHandler(project);
            new IdeaDriver(settings, project).convert(type, errorHandler, files);

            VirtualFile output = LocalFileSystem.getInstance().findFileByIoFile(new File(settings.getOutputDestination()));
            if (output != null) {
                output.refresh(false, true);
            }
        }
    }
}