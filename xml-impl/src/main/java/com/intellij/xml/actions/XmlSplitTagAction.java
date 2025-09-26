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
package com.intellij.xml.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.*;

import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.split.current.tag", fileExtensions = "html", categories = "XML")
public class XmlSplitTagAction implements IntentionAction {
    @Override
    @Nonnull
    public String getText() {
        return XmlLocalize.xmlSplitTagIntentionAction().get();
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        if (file instanceof XmlFile xmlFile && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement psiElement = xmlFile.findElementAt(offset);
            if (psiElement != null && psiElement.getParent() instanceof XmlText xmlText && xmlText.getText().trim().length() > 0) {
                PsiElement grandParent = xmlText.getParent();
                if (grandParent != null && !isInsideUnsplittableElement(grandParent)) {
                    return true;
                }
            }
        }

        return false;
    }

    @RequiredReadAction
    private static boolean isInsideUnsplittableElement(PsiElement grandParent) {
        if (!(grandParent instanceof HtmlTag) && grandParent.getContainingFile().getLanguage() != XHTMLLanguage.INSTANCE) {
            return false;
        }

        String name = ((XmlTag) grandParent).getName();
        return "html".equals(name) || "body".equals(name) || "title".equals(name);
    }

    @Override
    @RequiredWriteAction
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }

        if (editor != null) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement psiElement = file.findElementAt(offset);
            if (psiElement != null) {
                PsiElement xmlText = psiElement.getParent();
                TextRange textRange = xmlText.getTextRange();
                int offsetInElement = offset - textRange.getStartOffset();

                if (xmlText.getParent() instanceof XmlTag containingTag) {
                    String s = xmlText.getText();
                    String first = s.substring(0, offsetInElement);
                    String second = s.substring(offsetInElement);

                    if (xmlText instanceof XmlTagChild xmlTagChild) {
                        XmlTagChild prev = xmlTagChild.getPrevSiblingInTag();
                        while (prev != null) {
                            first = prev.getText() + first;
                            prev = prev.getPrevSiblingInTag();
                        }

                        XmlTagChild next = xmlTagChild.getNextSiblingInTag();
                        while (next != null) {
                            second += next.getText();
                            next = next.getNextSiblingInTag();
                        }
                    }

                    String filetext = buildNewText(containingTag, first, second);

                    XmlFile xmlFile = (XmlFile) PsiFileFactory.getInstance(project)
                        .createFileFromText("dummy.xml", XMLLanguage.INSTANCE, filetext);
                    PsiElement parent2 = containingTag.getParent();
                    XmlTag tag = xmlFile.getDocument().getRootTag();
                    XmlTag last = null;
                    PsiElement[] children = tag.getChildren();
                    for (int i = children.length - 1; i >= 0; i--) {
                        if (children[i] instanceof XmlTag element) {
                            XmlTag tag1 = (XmlTag) parent2.addAfter(element, containingTag);

                            if (last == null) {
                                last = tag1;
                            }
                        }
                    }

                    containingTag.delete();
                    editor.getCaretModel().moveToOffset(last.getValue().getTextRange().getStartOffset());
                }
            }
        }
    }

    private static String buildNewText(XmlTag xmlTag, String first, String second) {
        StringBuilder attrs = new StringBuilder();
        StringBuilder attrsWoId = new StringBuilder();
        for (XmlAttribute attribute : xmlTag.getAttributes()) {
            if (!"id".equals(attribute.getName())) {
                attrs.append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\" ");
                attrsWoId.append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\" ");
            }
            else {
                attrs.append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\" ");
            }
        }

        StringBuilder sb = new StringBuilder();
        String name = xmlTag.getName();
        sb.append("<root><").append(name);
        if (attrs.length() > 0) {
            sb.append(' ').append(attrs);
        }
        sb.append('>');
        sb.append(first);
        sb.append("</").append(name).append("><").append(name);
        if (attrsWoId.length() > 0) {
            sb.append(' ').append(attrsWoId);
        }
        sb.append('>');
        sb.append(second).append("</").append(name).append("></root>");

        return sb.toString();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
