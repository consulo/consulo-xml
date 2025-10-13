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
package com.intellij.xml.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.lexer.XmlLexer;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTagValue;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Maxim.Mossienko
 * @since 2006-06-29
 */
@ExtensionImpl
public class CheckValidXmlInScriptBodyInspection extends XmlSuppressableInspectionTool {
    private static final String SCRIPT_TAG_NAME = "script";
    private Lexer myXmlLexer;
    private static final String AMP_ENTITY_REFERENCE = "&amp;";
    private static final String LT_ENTITY_REFERENCE = "&lt;";

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitXmlTag(XmlTag tag) {
                if (SCRIPT_TAG_NAME.equals(tag.getName())
                    || (tag instanceof HtmlTag && SCRIPT_TAG_NAME.equalsIgnoreCase(tag.getName()))) {
                    PsiFile psiFile = tag.getContainingFile();
                    FileType fileType = psiFile.getFileType();

                    if (fileType == XHtmlFileType.INSTANCE) {
                        synchronized (CheckValidXmlInScriptBodyInspection.class) {
                            if (myXmlLexer == null) {
                                myXmlLexer = new XmlLexer();
                            }
                            XmlTagValue tagValue = tag.getValue();
                            String tagBodyText = tagValue.getText();

                            if (tagBodyText.length() > 0) {
                                myXmlLexer.start(tagBodyText);

                                while (myXmlLexer.getTokenType() != null) {
                                    IElementType tokenType = myXmlLexer.getTokenType();

                                    if (tokenType == XmlTokenType.XML_CDATA_START) {
                                        while (tokenType != null && tokenType != XmlTokenType.XML_CDATA_END) {
                                            myXmlLexer.advance();
                                            tokenType = myXmlLexer.getTokenType();
                                        }
                                        if (tokenType == null) {
                                            break;
                                        }
                                    }
                                    if (tokenType == XmlTokenType.XML_BAD_CHARACTER && "&".equals(TreeUtil.getTokenText(myXmlLexer))
                                        || tokenType == XmlTokenType.XML_START_TAG_START) {
                                        int valueStart = tagValue.getTextRange().getStartOffset();
                                        int offset = valueStart + myXmlLexer.getTokenStart();
                                        PsiElement psiElement = psiFile.findElementAt(offset);
                                        TextRange elementRange = psiElement.getTextRange();

                                        int offsetInElement = offset - elementRange.getStartOffset();
                                        holder.newProblem(XmlLocalize.unescapedXmlCharacter())
                                            .range(psiElement)
                                            .withFix(new InsertQuotedCharacterQuickFix(psiFile, psiElement, offsetInElement))
                                            .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                                            .create();

                                        int endOfElementInScriptTag = elementRange.getEndOffset() - valueStart;
                                        while (myXmlLexer.getTokenEnd() < endOfElementInScriptTag) {
                                            myXmlLexer.advance();
                                            if (myXmlLexer.getTokenType() == null) {
                                                break;
                                            }
                                        }
                                    }
                                    myXmlLexer.advance();
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlInspectionGroupNames.HTML_INSPECTIONS;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.htmlInspectionsCheckValidScriptTag();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "CheckValidXmlInScriptTagBody";
    }

    private static class InsertQuotedCharacterQuickFix implements LocalQuickFix {
        private final PsiFile myPsiFile;
        private final PsiElement myPsiElement;
        private final int myStartInElement;

        public InsertQuotedCharacterQuickFix(PsiFile psiFile, PsiElement psiElement, int startInElement) {
            myPsiFile = psiFile;
            myPsiElement = psiElement;
            myStartInElement = startInElement;
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public LocalizeValue getName() {
            String character = getXmlCharacter();
            return XmlLocalize.unescapedXmlCharacterFixMessage(
                character.equals("&") ? XmlLocalize.unescapedXmlCharacterFixMessageParameter().get() : character
            );
        }

        @Override
        @RequiredUIAccess
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor problemDescriptor) {
            if (!FileModificationService.getInstance().prepareFileForWrite(myPsiFile)) {
                return;
            }
            TextRange range = myPsiElement.getTextRange();
            OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(project)
                .builder(myPsiFile.getVirtualFile())
                .offset(range.getStartOffset() + myStartInElement)
                .build();

            Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            if (editor == null) {
                return;
            }

            String xmlCharacter = getXmlCharacter();
            String replacement = xmlCharacter.equals("&") ? AMP_ENTITY_REFERENCE : LT_ENTITY_REFERENCE;
            replacement = myPsiElement.getText().replace(xmlCharacter, replacement);

            editor.getDocument().replaceString(
                range.getStartOffset(),
                range.getEndOffset(),
                replacement
            );
        }

        @RequiredReadAction
        private String getXmlCharacter() {
            return myPsiElement.getText().substring(myStartInElement, myStartInElement + 1);
        }
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
