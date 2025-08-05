// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xml.actions;

import com.intellij.xml.Html5SchemaProvider;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.xml.impl.localize.XmlLocalize;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlEntityDecl;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;

/**
 * @author Dennis.Ushakov
 */
@ActionImpl(
    id = "EscapeEntities",
    parents = @ActionParentRef(value = @ActionRef(id = "EditMenu"), anchor = ActionRefAnchor.LAST)
)
public class EscapeEntitiesAction extends BaseCodeInsightAction implements CodeInsightActionHandler {
    public EscapeEntitiesAction() {
        super(XmlLocalize.actionEscapeEntitiesText(), XmlLocalize.actionEscapeEntitiesDescription());
    }

    @RequiredReadAction
    private static String escape(XmlFile file, IntObjectMap<String> map, String text, int start) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            PsiElement element = file.findElementAt(start + i);
            if (element != null && isCharacterElement(element)) {
                if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'' || c > 0x7f) {
                    String escape = map.get(c);
                    if (escape != null) {
                        result.append("&").append(escape).append(";");
                        continue;
                    }
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    @Nonnull
    private static IntObjectMap<String> computeMap(XmlFile xmlFile) {
        XmlFile file = XmlUtil.findXmlFile(xmlFile, Html5SchemaProvider.getCharsDtdLocation());
        assert file != null;

        IntObjectMap<String> result = IntMaps.newIntObjectHashMap();
        XmlUtil.processXmlElements(
            file,
            element -> {
                if (element instanceof XmlEntityDecl xmlEntityDecl) {
                    String value = xmlEntityDecl.getValueElement().getValue();
                    int key = Integer.parseInt(value.substring(2, value.length() - 1));
                    if (!result.containsKey(key)) {
                        result.put(key, xmlEntityDecl.getName());
                    }
                }
                return true;
            },
            true
        );
        return result;
    }

    @RequiredReadAction
    private static boolean isCharacterElement(PsiElement element) {
        IElementType type = element.getNode().getElementType();
        if (type == XmlTokenType.XML_DATA_CHARACTERS) {
            return true;
        }
        if (type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN && element.getParent().getParent() instanceof XmlAttribute) {
            return true;
        }
        if (type == XmlTokenType.XML_BAD_CHARACTER) {
            return true;
        }
        if (type == XmlTokenType.XML_START_TAG_START) {
            if (element.getNextSibling() instanceof PsiErrorElement || element.getParent() instanceof PsiErrorElement) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return file instanceof XmlFile;
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return this;
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        int[] starts = editor.getSelectionModel().getBlockSelectionStarts();
        int[] ends = editor.getSelectionModel().getBlockSelectionEnds();
        Document document = editor.getDocument();
        XmlFile xmlFile = (XmlFile) file;
        IntObjectMap<String> map = computeMap(xmlFile);
        for (int i = starts.length - 1; i >= 0; i--) {
            int start = starts[i];
            int end = ends[i];
            String oldText = document.getText(new TextRange(start, end));
            String newText = escape(xmlFile, map, oldText, start);
            if (!oldText.equals(newText)) {
                document.replaceString(start, end, newText);
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
