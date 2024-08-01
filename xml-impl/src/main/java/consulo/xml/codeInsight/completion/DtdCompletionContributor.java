/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.xml.lang.dtd.DTDLanguage;
import consulo.xml.psi.xml.XmlEntityDecl;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl(id = "dtd")
public class DtdCompletionContributor extends CompletionContributor {
    private static final String[] KEYWORDS = new String[]{
        "#PCDATA",
        "#IMPLIED",
        "#REQUIRED",
        "#FIXED",
        "<!ATTLIST",
        "<!ELEMENT",
        "<!NOTATION",
        "INCLUDE",
        "IGNORE",
        "CDATA",
        "ID",
        "IDREF",
        "EMPTY",
        "ANY",
        "IDREFS",
        "ENTITIES",
        "ENTITY",
        "<!ENTITY",
        "NMTOKEN",
        "NMTOKENS",
        "SYSTEM",
        "PUBLIC"
    };

    private static final InsertHandler<LookupElement> INSERT_HANDLER = new InsertHandler<LookupElement>() {
        @Override
        public void handleInsert(InsertionContext context, LookupElement item) {
            if (item.getObject().toString().startsWith("<!")) {
                context.commitDocument();

                int caretOffset = context.getEditor().getCaretModel().getOffset();
                PsiElement tag = PsiTreeUtil.getParentOfType(context.getFile().findElementAt(caretOffset), PsiNamedElement.class);

                if (tag == null) {
                    context.getEditor().getDocument().insertString(caretOffset, " >");
                    context.getEditor().getCaretModel().moveToOffset(caretOffset + 1);
                }
            }
        }
    };

    public DtdCompletionContributor() {
        extend(
            CompletionType.BASIC,
            psiElement(),
            (parameters, context, result) -> {
                PsiElement position = parameters.getPosition();
                PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
                if (prev != null && hasDtdKeywordCompletion(prev)) {
                    addKeywordCompletions(result.withPrefixMatcher(keywordPrefix(position, result.getPrefixMatcher().getPrefix())));
                }
                if (prev != null && prev.textMatches("%")) {
                    addEntityCompletions(result, position);
                }
            }
        );
    }

    @Nonnull
    private static String keywordPrefix(@Nonnull PsiElement position, @Nonnull String prefix) {
        final PsiElement prevLeaf = PsiTreeUtil.prevLeaf(position);
        final PsiElement prevPrevLeaf = prevLeaf != null ? PsiTreeUtil.prevLeaf(prevLeaf) : null;

        if (prevLeaf != null) {
            final String prevLeafText = prevLeaf.getText();

            if ("#".equals(prevLeafText)) {
                prefix = "#" + prefix;
            }
            else if ("!".equals(prevLeafText) && prevPrevLeaf != null && "<".equals(prevPrevLeaf.getText())) {
                prefix = "<!" + prefix;
            }
        }

        return prefix;

    }

    private static void addKeywordCompletions(@Nonnull CompletionResultSet result) {
        for (String keyword : KEYWORDS) {
            result.addElement(LookupElementBuilder.create(keyword).withInsertHandler(INSERT_HANDLER));
        }
    }

    private static void addEntityCompletions(@Nonnull final CompletionResultSet result, PsiElement position) {
        final PsiElementProcessor processor = element -> {
            if (element instanceof XmlEntityDecl xmlEntityDecl) {
                String name = xmlEntityDecl.getName();
                if (name != null && xmlEntityDecl.isInternalReference()) {
                    result.addElement(
                        LookupElementBuilder.create(name).withInsertHandler(XmlCompletionContributor.ENTITY_INSERT_HANDLER)
                    );
                }
            }
            return true;
        };
        XmlUtil.processXmlElements((XmlFile)position.getContainingFile().getOriginalFile(), processor, true);
    }

    private static boolean hasDtdKeywordCompletion(@Nonnull PsiElement prev) {
        return prev.textMatches("#") || prev.textMatches("!") || prev.textMatches("(")
            || prev.textMatches(",") || prev.textMatches("|") || prev.textMatches("[")
            || prev.getNode().getElementType() == XmlTokenType.XML_NAME;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return DTDLanguage.INSTANCE;
    }
}
