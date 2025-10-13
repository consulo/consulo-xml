/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.xml.codeInsight.daemon.impl.tagTreeHighlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlTagTreeHighlightingPass extends TextEditorHighlightingPass {
    private static final Key<List<RangeHighlighter>> TAG_TREE_HIGHLIGHTERS_IN_EDITOR_KEY =
        Key.create("TAG_TREE_HIGHLIGHTERS_IN_EDITOR_KEY");

    private static final TextAttributesKey TAG_TREE_HIGHLIGHTING_KEY = TextAttributesKey.of("TAG_TREE_HIGHLIGHTING_KEY");
    private static final HighlightInfoType TYPE =
        new HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, TAG_TREE_HIGHLIGHTING_KEY);

    private final PsiFile myFile;
    private final EditorEx myEditor;

    private final List<Couple<TextRange>> myPairsToHighlight = new ArrayList<>();

    public XmlTagTreeHighlightingPass(@Nonnull PsiFile file, @Nonnull EditorEx editor) {
        super(file.getProject(), editor.getDocument(), true);
        myFile = file;
        myEditor = editor;
    }

    @RequiredReadAction
    @Override
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        if (Application.get().isUnitTestMode()) {
            return;
        }

        if (!XmlEditorOptions.getInstance().isTagTreeHighlightingEnabled()) {
            return;
        }

        int offset = myEditor.getCaretModel().getOffset();
        PsiElement[] elements = null; // FIXME [VISTALL] for now idk when it used, due it based on breadcrumbs, which removed

        if (elements == null || elements.length == 0 || !XmlTagTreeHighlightingUtil.containsTagsWithSameName(elements)) {
            elements = PsiElement.EMPTY_ARRAY;
            FileViewProvider provider = myFile.getViewProvider();
            for (Language language : provider.getLanguages()) {
                PsiElement element = provider.findElementAt(offset, language);
                if (!isTagStartOrEnd(element)) {
                    element = null;
                }
                if (element == null && offset > 0) {
                    element = provider.findElementAt(offset - 1, language);
                    if (!isTagStartOrEnd(element)) {
                        element = null;
                    }
                }

                XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
                if (tag != null) {
                    elements = new PsiElement[]{tag};
                }
            }
        }

        for (int i = elements.length - 1; i >= 0; i--) {
            if (elements[i] instanceof XmlTag) {
                myPairsToHighlight.add(getTagRanges((XmlTag)elements[i]));
            }
        }
    }

    @RequiredReadAction
    private static boolean isTagStartOrEnd(@Nullable PsiElement element) {
        if (element == null) {
            return false;
        }
        IElementType type = element.getNode().getElementType();
        if (type == XmlTokenType.XML_NAME) {
            return isTagStartOrEnd(element.getNextSibling()) || isTagStartOrEnd(element.getPrevSibling());
        }
        return type == XmlTokenType.XML_START_TAG_START || type == XmlTokenType.XML_END_TAG_START || type == XmlTokenType.XML_TAG_END;
    }

    @Nonnull
    private static Couple<TextRange> getTagRanges(XmlTag tag) {
        ASTNode tagNode = tag.getNode();
        return Couple.of(getStartTagRange(tagNode), getEndTagRange(tagNode));
    }

    @Nullable
    private static TextRange getStartTagRange(ASTNode tagNode) {
        ASTNode startTagStart = XmlChildRole.START_TAG_START_FINDER.findChild(tagNode);
        if (startTagStart == null) {
            return null;
        }

        ASTNode tagName = startTagStart.getTreeNext();
        if (tagName == null || tagName.getElementType() != XmlTokenType.XML_NAME) {
            return null;
        }

        ASTNode next = tagName.getTreeNext();
        if (next != null && next.getElementType() == XmlTokenType.XML_TAG_END) {
            tagName = next;
        }

        return new TextRange(startTagStart.getStartOffset(), tagName.getTextRange().getEndOffset());
    }

    @Nullable
    private static TextRange getEndTagRange(ASTNode tagNode) {
        ASTNode endTagStart = XmlChildRole.CLOSING_TAG_START_FINDER.findChild(tagNode);
        if (endTagStart == null) {
            return null;
        }

        ASTNode endTagEnd = endTagStart;
        while (endTagEnd != null && endTagEnd.getElementType() != XmlTokenType.XML_TAG_END) {
            endTagEnd = endTagEnd.getTreeNext();
        }

        if (endTagEnd == null) {
            return null;
        }

        return new TextRange(endTagStart.getStartOffset(), endTagEnd.getTextRange().getEndOffset());
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        if (myDocument != null) {
            List<HighlightInfo> infos = getHighlights();
            UpdateHighlightersUtil.setHighlightersToEditor(
                myProject,
                myDocument,
                0,
                myFile.getTextLength(),
                infos,
                getColorsScheme(),
                getId()
            );
        }
    }

    public List<HighlightInfo> getHighlights() {
        clearLineMarkers(myEditor);

        int count = myPairsToHighlight.size();
        List<HighlightInfo> highlightInfos = new ArrayList<>(count * 2);
        MarkupModel markupModel = myEditor.getMarkupModel();

        ColorValue[] baseColors = XmlTagTreeHighlightingUtil.getBaseColors();
        ColorValue[] colorsForEditor = count > 1 ? toColorsForEditor(baseColors) : new ColorValue[]{
            myEditor.getColorsScheme().getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES).getBackgroundColor()
        };
        ColorValue[] colorsForLineMarkers = toColorsForLineMarkers(baseColors);

        List<RangeHighlighter> newHighlighters = new ArrayList<>();

        assert colorsForEditor.length > 0;

        for (int i = 0; i < count && i < baseColors.length; i++) {
            Couple<TextRange> pair = myPairsToHighlight.get(i);

            if (pair.first == null && pair.second == null) {
                continue;
            }

            ColorValue color = colorsForEditor[i];

            if (color == null) {
                continue;
            }

            if (pair.first != null && !pair.first.isEmpty()) {
                highlightInfos.add(createHighlightInfo(color, pair.first));
            }

            if (pair.second != null && !pair.second.isEmpty()) {
                highlightInfos.add(createHighlightInfo(color, pair.second));
            }

            int start = pair.first != null ? pair.first.getStartOffset() : pair.second.getStartOffset();
            int end = pair.second != null ? pair.second.getEndOffset() : pair.first.getEndOffset();

            ColorValue lineMarkerColor = colorsForLineMarkers[i];
            if (count > 1 && lineMarkerColor != null && start != end) {
                RangeHighlighter highlighter = createHighlighter(markupModel, new TextRange(start, end), lineMarkerColor);
                newHighlighters.add(highlighter);
            }
        }

        myEditor.putUserData(TAG_TREE_HIGHLIGHTERS_IN_EDITOR_KEY, newHighlighters);

        return highlightInfos;
    }

    private static void clearLineMarkers(Editor editor) {
        List<RangeHighlighter> oldHighlighters = editor.getUserData(TAG_TREE_HIGHLIGHTERS_IN_EDITOR_KEY);

        if (oldHighlighters != null) {
            MarkupModelEx markupModel = (MarkupModelEx)editor.getMarkupModel();

            for (RangeHighlighter highlighter : oldHighlighters) {
                if (markupModel.containsHighlighter(highlighter)) {
                    highlighter.dispose();
                }
            }
            editor.putUserData(TAG_TREE_HIGHLIGHTERS_IN_EDITOR_KEY, null);
        }
    }

    @Nonnull
    private static HighlightInfo createHighlightInfo(ColorValue color, @Nonnull TextRange range) {
        TextAttributes attributes = new TextAttributes(null, color, null, null, Font.PLAIN);
        return HighlightInfo.newHighlightInfo(TYPE)
            .range(range)
            .textAttributes(attributes)
            .severity(HighlightSeverity.INFORMATION)
            .createUnconditionally();
    }

    @Nonnull
    private static RangeHighlighter createHighlighter(MarkupModel mm, @Nonnull TextRange range, ColorValue color) {
        RangeHighlighter highlighter =
            mm.addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), 0, null, HighlighterTargetArea.LINES_IN_RANGE);

        highlighter.setLineMarkerRenderer((editor, g, r) ->
        {
            g.setColor(TargetAWT.to(color));
            g.fillRect(r.x - 1, r.y, 2, r.height);
        });
        return highlighter;
    }

    static ColorValue toLineMarkerColor(int gray, @Nullable ColorValue c) {
        RGBColor color = c == null ? null : c.toRGB();
        return color == null ? null : new RGBColor(
            toLineMarkerColor(gray, color.getRed()),
            toLineMarkerColor(gray, color.getGreen()),
            toLineMarkerColor(gray, color.getBlue())
        );
    }

    private static int toLineMarkerColor(int gray, int color) {
        int value = (int)(gray * 0.6 + 0.32 * color);
        return value < 0 ? 0 : value > 255 ? 255 : value;
    }

    private static ColorValue[] toColorsForLineMarkers(ColorValue[] baseColors) {
        ColorValue[] colors = new ColorValue[baseColors.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = toLineMarkerColor(239, baseColors[i]);
        }
        return colors;
    }

    private ColorValue[] toColorsForEditor(ColorValue[] baseColors) {
        ColorValue tagBackground = myEditor.getBackgroundColor();

        if (tagBackground == null) {
            return baseColors;
        }

        ColorValue[] resultColors = new ColorValue[baseColors.length];
        // todo: make configurable
        double transparency = XmlEditorOptions.getInstance().getTagTreeHighlightingOpacity() * 0.01;

        for (int i = 0; i < resultColors.length; i++) {
            ColorValue color = baseColors[i];

            ColorValue color1 = color != null ? XmlTagTreeHighlightingUtil.makeTransparent(color, tagBackground, transparency) : null;
            resultColors[i] = color1;
        }

        return resultColors;
    }

    public static void clearHighlightingAndLineMarkers(Editor editor, @Nonnull Project project) {
        MarkupModel markupModel = DocumentMarkupModel.forDocument(editor.getDocument(), project, true);

        for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
            Object tooltip = highlighter.getErrorStripeTooltip();

            if (tooltip instanceof HighlightInfo highlightInfo && highlightInfo.getType() == TYPE) {
                highlighter.dispose();
            }
        }

        clearLineMarkers(editor);
    }
}
