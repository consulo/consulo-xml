// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.relaxNG.validation;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.actions.validate.ValidateXmlHandler;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.util.UriOrFile;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.prop.rng.RngProperty;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.psi.xml.XmlFile;
import jakarta.annotation.Nullable;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.model.descriptors.RngElementDescriptor;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.Future;

@ExtensionImpl
public class RngValidateHandler implements ValidateXmlHandler {
    private static final String CONTENT_NAME = "Validate RELAX NG";
    private static final Key<NewErrorTreeViewPanel> KEY = Key.create("VALIDATING");

    @Override
    public void doValidate(XmlFile file) {
        final XmlFile schema = getRngSchema(file);
        if (schema == null) {
            return;
        }

        final VirtualFile instanceFile = file.getVirtualFile();
        final VirtualFile schemaFile = schema.getVirtualFile();
        if (instanceFile == null || schemaFile == null) {
            return;
        }

        doRun(file.getProject(), instanceFile, schemaFile);
    }

    @Override
    public boolean isAvailable(XmlFile file) {
        return getRngSchema(file) != null;
    }

    @Nullable
    private static XmlFile getRngSchema(XmlFile file) {
        final RngElementDescriptor descriptor = getRootDescriptor(file);
        if (descriptor == null) {
            return null;
        }

        final PsiElement element = descriptor.getDeclaration();
        final XmlFile schema = PsiTreeUtil.getParentOfType(element, XmlFile.class);
        if (schema == null) {
            return null;
        }
        return schema;
    }

    private static RngElementDescriptor getRootDescriptor(PsiFile file) {
        try {
            if (file instanceof XmlFile) {
                final XmlElementDescriptor descriptor = ((XmlFile) file).getDocument().getRootTag().getDescriptor();
                if (descriptor instanceof RngElementDescriptor) {
                    return (RngElementDescriptor) descriptor;
                }
            }
        }
        catch (NullPointerException e1) {
            // OK
        }
        return null;
    }

    private static void doRun(final Project project, final VirtualFile instanceFile, final VirtualFile schemaFile) {
        saveFiles(instanceFile, schemaFile);

        final MessageViewHelper helper = new MessageViewHelper(project, CONTENT_NAME, KEY);

        helper.openMessageView(() -> doRun(project, instanceFile, schemaFile));

        final Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(
            () -> ApplicationManager.getApplication().runReadAction(() -> {
                MessageViewHelper.ErrorHandler eh = helper.new ErrorHandler();

                doValidation(instanceFile, schemaFile, eh);

                SwingUtilities.invokeLater(() -> {
                        if (!eh.hadErrorOrWarning()) {
                            helper.close();
                        }
                    }
                );
            }));

        helper.setProcessController(new NewErrorTreeViewPanel.ProcessController() {
            @Override
            public void stopProcess() {
                future.cancel(true);
            }

            @Override
            public boolean isProcessStopped() {
                return future.isDone();
            }
        });
    }

    private static void doValidation(VirtualFile instanceFile, VirtualFile schemaFile, org.xml.sax.ErrorHandler eh) {
        final SchemaReader sr = FileTypeRegistry.getInstance().isFileOfType(schemaFile, RncFileType.getInstance()) ?
            CompactSchemaReader.getInstance() :
            new AutoSchemaReader();

        final PropertyMapBuilder properties = new PropertyMapBuilder();
        ValidateProperty.ERROR_HANDLER.put(properties, eh);

        // TODO: should some options dialog displayed before validating?
        RngProperty.CHECK_ID_IDREF.add(properties);

        try {
            final String schemaPath = VirtualFileUtil.fixIDEAUrl(schemaFile.getUrl());
            try {
                final ValidationDriver driver = new ValidationDriver(properties.toPropertyMap(), sr);
                final InputSource in = ValidationDriver.uriOrFileInputSource(schemaPath);
                in.setEncoding(schemaFile.getCharset().name());

                if (driver.loadSchema(in)) {
                    final String path = VirtualFileUtil.fixIDEAUrl(instanceFile.getUrl());
                    try {
                        driver.validate(ValidationDriver.uriOrFileInputSource(path));
                    }
                    catch (IOException e1) {
                        eh.fatalError(new SAXParseException(e1.getMessage(), null, UriOrFile.fileToUri(path), -1, -1, e1));
                    }
                }
            }
            catch (SAXParseException e1) {
                eh.fatalError(e1);
            }
            catch (IOException e1) {
                eh.fatalError(new SAXParseException(e1.getMessage(), null, UriOrFile.fileToUri(schemaPath), -1, -1, e1));
            }
        }
        catch (SAXException /*| MalformedURLException*/ e1) {
            // huh?
            Logger.getInstance(RngValidateHandler.class).error(e1);
        }
    }

    public static void saveFiles(VirtualFile... files) {
        // ensure the validation/conversion runs on the current content
        final FileDocumentManager mgr = FileDocumentManager.getInstance();
        for (VirtualFile f : files) {
            final Document document = mgr.getDocument(f);
            if (document != null) {
                mgr.saveDocument(document);
            }
        }
    }
}
