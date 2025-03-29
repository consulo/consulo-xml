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

import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlNamespaceHelper;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.document.Document;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;

/**
 * @author peter
 */
public class XmlAttributeInsertHandler implements InsertHandler<LookupElement> {
    private static final Logger LOG = Logger.getInstance(XmlAttributeInsertHandler.class);

    public static final XmlAttributeInsertHandler INSTANCE = new XmlAttributeInsertHandler();

    private final String myNamespaceToInsert;

    public XmlAttributeInsertHandler() {
        this(null);
    }

    public XmlAttributeInsertHandler(@Nullable String namespaceToInsert) {
        myNamespaceToInsert = namespaceToInsert;
    }

    @Override
    @RequiredUIAccess
    @RequiredWriteAction
    public void handleInsert(InsertionContext context, LookupElement item) {
        Editor editor = context.getEditor();

        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();
        PsiFile file = context.getFile();

        CharSequence chars = document.getCharsSequence();
        boolean insertQuotes = XmlEditorOptions.getInstance().isInsertQuotesForAttributeValue();
        boolean hasQuotes = CharArrayUtil.regionMatches(chars, caretOffset, "=\"");
        if (!hasQuotes && !CharArrayUtil.regionMatches(chars, caretOffset, "='")) {
            PsiElement fileContext = file.getContext();
            String toInsert = "=\"\"";

            if (fileContext != null && fileContext.getText().startsWith("\"")) {
                toInsert = "=''";
            }

            if (!insertQuotes) {
                toInsert = "=";
            }

            if (caretOffset >= document.getTextLength() || "/> \n\t\r".indexOf(document.getCharsSequence().charAt(caretOffset)) < 0) {
                document.insertString(caretOffset, toInsert + " ");
            }
            else {
                document.insertString(caretOffset, toInsert);
            }

            if ('=' == context.getCompletionChar()) {
                context.setAddCompletionChar(false); // IDEA-19449
            }
        }

        editor.getCaretModel().moveToOffset(caretOffset + (insertQuotes || hasQuotes ? 2 : 1));
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        editor.getSelectionModel().removeSelection();
        AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);

        if (myNamespaceToInsert != null && file instanceof XmlFile) {
            PsiElement element = file.findElementAt(context.getStartOffset());
            XmlTag tag = element != null ? PsiTreeUtil.getParentOfType(element, XmlTag.class) : null;

            if (tag != null) {
                String prefix = ExtendedTagInsertHandler.suggestPrefix((XmlFile)file, myNamespaceToInsert);

                if (prefix != null) {
                    prefix = makePrefixUnique(prefix, tag);
                    XmlNamespaceHelper helper = XmlNamespaceHelper.getHelper(context.getFile());

                    if (helper != null) {
                        Project project = context.getProject();
                        PsiDocumentManager.getInstance(project).commitDocument(document);
                        qualifyWithPrefix(prefix, element);
                        helper.insertNamespaceDeclaration((XmlFile)file, editor, Collections.singleton(myNamespaceToInsert), prefix, null);
                    }
                }
            }
        }
    }

    @RequiredWriteAction
    private static void qualifyWithPrefix(@Nonnull String namespacePrefix, @Nonnull PsiElement context) {
        PsiElement parent = context.getParent();

        if (parent instanceof XmlAttribute) {
            XmlAttribute attribute = (XmlAttribute)parent;
            String prefix = attribute.getNamespacePrefix();

            if (!prefix.equals(namespacePrefix) && StringUtil.isNotEmpty(namespacePrefix)) {
                String name = namespacePrefix + ":" + attribute.getLocalName();
                try {
                    attribute.setName(name);
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }
        }
    }

    @Nonnull
    private static String makePrefixUnique(@Nonnull String basePrefix, @Nonnull XmlTag context) {
        if (context.getNamespaceByPrefix(basePrefix).isEmpty()) {
            return basePrefix;
        }
        int i = 1;

        while (!context.getNamespaceByPrefix(basePrefix + i).isEmpty()) {
            i++;
        }
        return basePrefix + i;
    }
}
