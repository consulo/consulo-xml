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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.XmlNamespaceHelper;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.AutoImportHelper;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.highlight.VisibleHighlightingPassFactory;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.HintAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiAnchor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.stub.IdTableBuilding;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.JBPopup;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.xml.application.options.XmlSettings;
import consulo.xml.codeInsight.completion.ExtendedTagInsertHandler;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Maxim.Mossienko
 */
@IntentionMetaData(ignoreId = "xml.create.ns.declaration", fileExtensions = "xml", categories = "XML")
public class CreateNSDeclarationIntentionFix implements HintAction, LocalQuickFix {
    private static final Logger LOG = Logger.getInstance(CreateNSDeclarationIntentionFix.class);

    private final String myNamespacePrefix;
    private final PsiAnchor myElement;
    private final PsiAnchor myToken;

    @Nonnull
    private XmlFile getFile() {
        return (XmlFile)myElement.getFile();
    }

    @Nullable
    @RequiredReadAction
    public static CreateNSDeclarationIntentionFix createFix(@Nonnull PsiElement element, @Nonnull String namespacePrefix) {
        PsiFile file = element.getContainingFile();
        return file instanceof XmlFile ? new CreateNSDeclarationIntentionFix(element, namespacePrefix) : null;
    }

    @RequiredReadAction
    protected CreateNSDeclarationIntentionFix(@Nonnull PsiElement element, @Nonnull String namespacePrefix) {
        this(element, namespacePrefix, null);
    }

    @RequiredReadAction
    public CreateNSDeclarationIntentionFix(@Nonnull PsiElement element, String namespacePrefix, @Nullable XmlToken token) {
        myNamespacePrefix = namespacePrefix;
        myElement = PsiAnchor.create(element);
        myToken = token == null ? null : PsiAnchor.create(token);
    }

    @Override
    @Nonnull
    public String getText() {
        String alias = StringUtil.capitalize(getXmlExtension().getNamespaceAlias(getFile()));
        return XmlErrorLocalize.createNamespaceDeclarationQuickfix(alias).get();
    }

    private XmlNamespaceHelper getXmlExtension() {
        return XmlNamespaceHelper.getHelper(getFile());
    }

    @Override
    @Nonnull
    public String getName() {
        return getFamilyName();
    }

    @Override
    @Nonnull
    public String getFamilyName() {
        return getText();
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiFile containingFile = descriptor.getPsiElement().getContainingFile();
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        PsiFile file = editor != null ? PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) : null;
        if (file == null || !Objects.equals(file.getVirtualFile(), containingFile.getVirtualFile())) {
            return;
        }

        try {
            invoke(project, editor, containingFile);
        }
        catch (IncorrectOperationException ex) {
            LOG.error(ex);
        }
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        PsiElement element = myElement.retrieve();
        return element != null && element.isValid();
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }

        PsiElement element = myElement.retrieve();
        if (element == null) {
            return;
        }
        Set<String> set = getXmlExtension().guessUnboundNamespaces(element, getFile());
        String[] namespaces = ArrayUtil.toStringArray(set);
        Arrays.sort(namespaces);

        runActionOverSeveralAttributeValuesAfterLettingUserSelectTheNeededOne(
            namespaces,
            project,
            namespace -> {
                String prefix = myNamespacePrefix;
                if (StringUtil.isEmpty(prefix)) {
                    XmlFile xmlFile = XmlExtension.getExtension(file).getContainingFile(element);
                    prefix = ExtendedTagInsertHandler.getPrefixByNamespace(xmlFile, namespace);
                    if (StringUtil.isNotEmpty(prefix)) {
                        // namespace already declared
                        ExtendedTagInsertHandler.qualifyWithPrefix(prefix, element);
                        return;
                    }
                    else {
                        prefix = ExtendedTagInsertHandler.suggestPrefix(xmlFile, namespace);
                        if (!StringUtil.isEmpty(prefix)) {
                            ExtendedTagInsertHandler.qualifyWithPrefix(prefix, element);
                            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
                        }
                    }
                }
                int offset = editor.getCaretModel().getOffset();
                RangeMarker marker = editor.getDocument().createRangeMarker(offset, offset);
                XmlNamespaceHelper helper = XmlNamespaceHelper.getHelper(file);
                helper.insertNamespaceDeclaration((XmlFile)file, editor, Collections.singleton(namespace), prefix,
                    param -> {
                        if (!namespace.isEmpty()) {
                            editor.getCaretModel().moveToOffset(marker.getStartOffset());
                        }
                    }
                );
            },
            getTitle(),
            this,
            editor
        );
    }

    private String getTitle() {
        return XmlErrorLocalize.selectNamespaceTitle(StringUtil.capitalize(getXmlExtension().getNamespaceAlias(getFile()))).get();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public boolean showHint(@Nonnull Editor editor) {
        if (myToken == null) {
            return false;
        }
        XmlToken token = (XmlToken)myToken.retrieve();
        if (token == null) {
            return false;
        }
        if (!XmlSettings.getInstance().SHOW_XML_ADD_IMPORT_HINTS || myNamespacePrefix.isEmpty()) {
            return false;
        }
        PsiElement element = myElement.retrieve();
        if (element == null) {
            return false;
        }
        Set<String> namespaces = getXmlExtension().guessUnboundNamespaces(element, getFile());
        if (!namespaces.isEmpty()) {
            String message = AutoImportHelper.getInstance(element.getProject())
                .getImportMessage(namespaces.size() > 1, namespaces.iterator().next());
            String title = getTitle();
            ImportNSAction action = new ImportNSAction(namespaces, getFile(), element, editor, title);
            if (element instanceof XmlTag) {
                if (VisibleHighlightingPassFactory.calculateVisibleRange(editor).contains(token.getTextRange())) {
                    HintManager.getInstance().showQuestionHint(
                        editor,
                        message,
                        token.getTextOffset(),
                        token.getTextOffset() + myNamespacePrefix.length(),
                        action
                    );
                    return true;
                }
            }
            else {
                HintManager.getInstance().showQuestionHint(
                    editor,
                    message,
                    element.getTextOffset(),
                    element.getTextRange().getEndOffset(),
                    action
                );
                return true;
            }
        }
        return false;
    }

    private static boolean checkIfGivenXmlHasTheseWords(String name, XmlFile tldFileByUri) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        List<String> list = StringUtil.getWordsIn(name);
        String[] words = ArrayUtil.toStringArray(list);
        boolean[] wordsFound = new boolean[words.length];
        int[] wordsFoundCount = new int[1];

        IdTableBuilding.ScanWordProcessor wordProcessor = (chars, charsArray, start, end) -> {
            if (wordsFoundCount[0] == words.length) {
                return;
            }
            int foundWordLen = end - start;

            Next:
            for (int i = 0; i < words.length; ++i) {
                String localName = words[i];
                if (wordsFound[i] || localName.length() != foundWordLen) {
                    continue;
                }

                for (int j = 0; j < localName.length(); ++j) {
                    if (chars.charAt(start + j) != localName.charAt(j)) {
                        continue Next;
                    }
                }

                wordsFound[i] = true;
                wordsFoundCount[0]++;
                break;
            }
        };

        CharSequence contents = tldFileByUri.getViewProvider().getContents();

        IdTableBuilding.scanWords(wordProcessor, contents, 0, contents.length());

        return wordsFoundCount[0] == words.length;
    }

    public interface StringToAttributeProcessor {
        void doSomethingWithGivenStringToProduceXmlAttributeNowPlease(@Nonnull String attrName) throws IncorrectOperationException;
    }

    @RequiredUIAccess
    public static void runActionOverSeveralAttributeValuesAfterLettingUserSelectTheNeededOne(
        @Nonnull String[] namespacesToChooseFrom,
        Project project,
        @RequiredUIAccess StringToAttributeProcessor onSelection,
        String title,
        IntentionAction requestor,
        Editor editor
    ) throws IncorrectOperationException {
        if (namespacesToChooseFrom.length > 1 && !Application.get().isUnitTestMode()) {
            JList list = new JBList(namespacesToChooseFrom);
            list.setCellRenderer(XmlNSRenderer.INSTANCE);
            Runnable runnable = () -> {
                int index = list.getSelectedIndex();
                if (index < 0) {
                    return;
                }
                PsiDocumentManager.getInstance(project).commitAllDocuments();

                CommandProcessor.getInstance().newCommand()
                    .project(project)
                    .name(LocalizeValue.ofNullable(requestor.getText()))
                    .groupId(requestor.getText())
                    .inWriteAction()
                    .run(() -> {
                        try {
                            onSelection.doSomethingWithGivenStringToProduceXmlAttributeNowPlease(namespacesToChooseFrom[index]);
                        }
                        catch (IncorrectOperationException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
            };

            JBPopup popup = new PopupChooserBuilder(list)
                .setTitle(title)
                .setItemChoosenCallback(runnable)
                .createPopup();
            editor.showPopupInBestPositionFor(popup);
        }
        else {
            onSelection.doSomethingWithGivenStringToProduceXmlAttributeNowPlease(
                namespacesToChooseFrom.length == 0 ? "" : namespacesToChooseFrom[0]
            );
        }
    }

    public static void processExternalUris(
        MetaHandler metaHandler,
        PsiFile file,
        ExternalUriProcessor processor,
        boolean showProgress
    ) {
        if (!showProgress || Application.get().isUnitTestMode()) {
            processExternalUrisImpl(metaHandler, file, processor);
        }
        else {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                () -> processExternalUrisImpl(metaHandler, file, processor),
                XmlErrorLocalize.findingAcceptableUri().get(),
                false,
                file.getProject()
            );
        }
    }

    public interface MetaHandler {
        boolean isAcceptableMetaData(PsiMetaData metadata, String url);

        String searchFor();
    }

    public static class TagMetaHandler implements MetaHandler {
        private String myName;

        public TagMetaHandler(String name) {
            myName = name;
        }

        @Override
        public boolean isAcceptableMetaData(PsiMetaData metaData, String url) {
            if (metaData instanceof XmlNSDescriptorImpl nsDescriptor) {
                XmlElementDescriptor descriptor = nsDescriptor.getElementDescriptor(searchFor(), url);
                return descriptor != null && !(descriptor instanceof AnyXmlElementDescriptor);
            }
            return false;
        }

        @Override
        public String searchFor() {
            return myName;
        }
    }

    private static void processExternalUrisImpl(MetaHandler metaHandler, PsiFile file, ExternalUriProcessor processor) {
        ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();

        String searchFor = metaHandler.searchFor();

        if (pi != null) {
            pi.setTextValue(XmlErrorLocalize.lookingInSchemas());
        }
        ExternalResourceManager instanceEx = ExternalResourceManager.getInstance();
        String[] availableUrls = instanceEx.getResourceUrls(null, true);
        int i = 0;

        for (String url : availableUrls) {
            if (pi != null) {
                pi.setFraction((double)i / availableUrls.length);
                pi.setText2(url);
                ++i;
            }
            XmlFile xmlFile = XmlUtil.findNamespace(file, url);

            if (xmlFile != null) {
                boolean wordFound = checkIfGivenXmlHasTheseWords(searchFor, xmlFile);
                if (!wordFound) {
                    continue;
                }
                XmlDocument document = xmlFile.getDocument();
                assert document != null;
                PsiMetaData metaData = document.getMetaData();

                if (metaHandler.isAcceptableMetaData(metaData, url)) {
                    XmlNSDescriptorImpl descriptor = metaData instanceof XmlNSDescriptorImpl nsDescriptor ? nsDescriptor : null;
                    String defaultNamespace = descriptor != null ? descriptor.getDefaultNamespace() : url;

                    // Skip rare stuff
                    if (!XmlUtil.XML_SCHEMA_URI2.equals(defaultNamespace) && !XmlUtil.XML_SCHEMA_URI3.equals(defaultNamespace)) {
                        processor.process(defaultNamespace, url);
                    }
                }
            }
        }
    }

    public interface ExternalUriProcessor {
        void process(@Nonnull String uri, @Nullable String url);
    }
}
