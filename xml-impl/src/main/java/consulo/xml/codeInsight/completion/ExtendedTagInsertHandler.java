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
package consulo.xml.codeInsight.completion;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.XmlNamespaceHelper;
import com.intellij.xml.XmlSchemaProvider;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class ExtendedTagInsertHandler extends XmlTagInsertHandler {
    private static final Logger LOG = Logger.getInstance(ExtendedTagInsertHandler.class);

    protected final String myElementName;
    @Nullable
    protected final String myNamespace;
    @Nullable
    protected final String myNamespacePrefix;

    public ExtendedTagInsertHandler(final String elementName, @Nullable final String namespace, @Nullable final String namespacePrefix) {
        myElementName = elementName;
        myNamespace = namespace;
        myNamespacePrefix = namespacePrefix;
    }

    @RequiredUIAccess
    @Override
    public void handleInsert(final InsertionContext context, final LookupElement item) {
        final XmlFile contextFile = (XmlFile)context.getFile();
        final XmlExtension extension = XmlExtension.getExtension(contextFile);
        final XmlFile file = extension.getContainingFile(contextFile);
        final Project project = context.getProject();

        assert file != null;
        final PsiElement psiElement = file.findElementAt(context.getStartOffset());
        assert psiElement != null;
        if (isNamespaceBound(psiElement)) {
            doDefault(context, item);
            return;
        }

        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        PsiDocumentManager.getInstance(project).commitDocument(document);

        final int caretOffset = editor.getCaretModel().getOffset();
        final RangeMarker caretMarker = document.createRangeMarker(caretOffset, caretOffset);
        caretMarker.setGreedyToRight(true);

        final XmlNamespaceHelper.Runner<String, IncorrectOperationException> runAfter = namespacePrefix -> {
            PsiDocumentManager.getInstance(project).commitDocument(document);
            final PsiElement element = file.findElementAt(context.getStartOffset());
            if (element != null) {
                qualifyWithPrefix(namespacePrefix, element, document);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            }
            editor.getCaretModel().moveToOffset(caretMarker.getEndOffset());
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
            doDefault(context, item);
        };

        try {
            final String prefixByNamespace = getPrefixByNamespace(file, myNamespace);
            if (myNamespacePrefix != null || StringUtil.isEmpty(prefixByNamespace)) {
                final String nsPrefix = myNamespacePrefix == null ? suggestPrefix(file, myNamespace) : myNamespacePrefix;
                XmlNamespaceHelper.getHelper(file)
                    .insertNamespaceDeclaration(file, editor, Collections.singleton(myNamespace), nsPrefix, runAfter);
                FeatureUsageTracker.getInstance().triggerFeatureUsed(XmlCompletionContributor.TAG_NAME_COMPLETION_FEATURE);
            }
            else {
                runAfter.run(prefixByNamespace);    // qualify && complete
            }
        }
        catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    protected void doDefault(final InsertionContext context, final LookupElement item) {
        ExtendedTagInsertHandler.super.handleInsert(context, item);
    }

    protected boolean isNamespaceBound(PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();
        if (!(parent instanceof XmlTag)) {
            return false;
        }
        final XmlTag tag = (XmlTag)parent;
        final XmlElementDescriptor tagDescriptor = tag.getDescriptor();
        final String tagNamespace = tag.getNamespace();
        assert myNamespace != null;
        return tagDescriptor != null && !(tagDescriptor instanceof AnyXmlElementDescriptor) && myNamespace.equals(tagNamespace);
    }

    @Nullable
    public static String getPrefixByNamespace(XmlFile file, final String namespace) {
        final XmlTag tag = file.getRootTag();
        return tag == null ? null : tag.getPrefixByNamespace(namespace);
    }

    @Nullable
    public static String suggestPrefix(XmlFile file, @Nullable String namespace) {
        if (namespace == null) {
            return null;
        }
        for (XmlSchemaProvider provider : XmlSchemaProvider.getAvailableProviders(file)) {
            String prefix = provider.getDefaultPrefix(namespace, file);
            if (prefix != null) {
                return prefix;
            }
        }
        return null;
    }

    protected Set<String> getNamespaces(final XmlFile file) {
        return XmlNamespaceHelper.getHelper(file).getNamespacesByTagName(myElementName, file);
    }

    protected void qualifyWithPrefix(final String namespacePrefix, final PsiElement element, final Document document) {
        qualifyWithPrefix(namespacePrefix, element);
    }

    public static void qualifyWithPrefix(final String namespacePrefix, final PsiElement element) {
        if (element.getParent() instanceof XmlTag tag) {
            final String prefix = tag.getNamespacePrefix();
            if (!prefix.equals(namespacePrefix) && StringUtil.isNotEmpty(namespacePrefix)) {
                final String name = namespacePrefix + ":" + tag.getLocalName();
                try {
                    tag.setName(name);
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }
        }
    }
}
