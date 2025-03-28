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
package consulo.xml.codeInsight.daemon.impl.quickfix;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.javaee.MapExternalResourceDialog;

import jakarta.annotation.Nonnull;

/**
 * @author mike
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.manually.setup.external.resource", fileExtensions = "xml", categories = "XML")
public class ManuallySetupExtResourceAction extends BaseExtResourceAction {
    protected String getQuickFixKeyId() {
        return "manually.setup.external.resource";
    }

    protected void doInvoke(@Nonnull final PsiFile file, final int offset, @Nonnull final String uri, final Editor editor)
        throws IncorrectOperationException {
        final MapExternalResourceDialog dialog = new MapExternalResourceDialog(uri, file.getProject(), file, null);
        dialog.show();
        if (dialog.isOK()) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    String location = dialog.getResourceLocation();
                    ExternalResourceManager.getInstance().addResource(dialog.getUri(), location);
                }
            });
        }
    }

    public boolean startInWriteAction() {
        return false;
    }
}
