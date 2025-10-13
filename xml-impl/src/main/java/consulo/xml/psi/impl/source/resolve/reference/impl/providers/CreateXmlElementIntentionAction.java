/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;
import com.intellij.xml.util.XmlUtil;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

class CreateXmlElementIntentionAction implements SyntheticIntentionAction {
    private final Function<String, LocalizeValue> myTextPattern;
    protected final TypeOrElementOrAttributeReference myRef;
    private boolean myIsAvailableEvaluated;
    private XmlFile myTargetFile;
    private final String myDeclarationTagName;

    CreateXmlElementIntentionAction(
        @Nonnull Function<String, LocalizeValue> textPattern,
        @Nonnull String declarationTagName,
        TypeOrElementOrAttributeReference ref
    ) {
        myTextPattern = textPattern;
        myRef = ref;
        myDeclarationTagName = declarationTagName;
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return myTextPattern.apply(XmlUtil.findLocalNameByQualifiedName(myRef.getCanonicalText()));
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        if (!myIsAvailableEvaluated) {
            XmlTag tag = PsiTreeUtil.getParentOfType(myRef.getElement(), XmlTag.class);
            if (tag != null) {
                XmlNSDescriptorImpl descriptor = myRef.getDescriptor(tag, myRef.getCanonicalText());

                if (descriptor != null
                    && descriptor.getDescriptorFile() != null
                    && descriptor.getDescriptorFile().isWritable()) {
                    myTargetFile = descriptor.getDescriptorFile();
                }
            }
            myIsAvailableEvaluated = true;
        }
        return myTargetFile != null;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }

        XmlTag rootTag = myTargetFile.getDocument().getRootTag();

        OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(project)
            .builder(myTargetFile.getVirtualFile())
            .offset(rootTag.getValue().getTextRange().getEndOffset())
            .build();

        Editor targetEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
        TemplateManager manager = TemplateManager.getInstance(project);
        Template template = manager.createTemplate("", "");

        addTextTo(template, rootTag);

        manager.startTemplate(targetEditor, template);
    }

    protected void addTextTo(Template template, XmlTag rootTag) {
        String schemaPrefix = rootTag.getPrefixByNamespace(XmlUtil.XML_SCHEMA_URI);
        if (!schemaPrefix.isEmpty()) {
            schemaPrefix += ":";
        }

        template.addTextSegment(
            "<" + schemaPrefix + myDeclarationTagName + " name=\"" + XmlUtil.findLocalNameByQualifiedName(myRef.getCanonicalText()) + "\">"
        );
        template.addEndVariable();
        template.addTextSegment(
            "</" + schemaPrefix + myDeclarationTagName + ">\n"
        );
        template.setToReformat(true);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
