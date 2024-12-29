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

/*
 * Created by IntelliJ IDEA.
 * User: spleaner
 * Date: Aug 8, 2007
 * Time: 2:20:33 PM
 */
package com.intellij.xml.refactoring;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.template.*;
import consulo.language.editor.template.event.TemplateEditingAdapter;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.Stack;
import consulo.util.lang.Pair;
import consulo.util.lang.function.PairProcessor;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class XmlTagInplaceRenamer {
    @NonNls
    private static final String PRIMARY_VARIABLE_NAME = "PrimaryVariable";
    @NonNls
    private static final String OTHER_VARIABLE_NAME = "OtherVariable";

    private final Editor myEditor;

    private final static Stack<XmlTagInplaceRenamer> ourRenamersStack = new Stack<XmlTagInplaceRenamer>();
    private ArrayList<RangeHighlighter> myHighlighters;

    private XmlTagInplaceRenamer(@Nonnull final Editor editor) {
        myEditor = editor;
    }

    public static void rename(final Editor editor, @Nonnull final XmlTag tag) {
        if (!ourRenamersStack.isEmpty()) {
            ourRenamersStack.peek().finish();
        }

        final XmlTagInplaceRenamer renamer = new XmlTagInplaceRenamer(editor);
        ourRenamersStack.push(renamer);
        renamer.rename(tag);
    }

    private void rename(@Nonnull final XmlTag tag) {
        final Pair<ASTNode, ASTNode> pair = getNamePair(tag);
        if (pair == null) {
            return;
        }

        final Project project = myEditor.getProject();
        if (project != null) {

            final List<TextRange> highlightRanges = new ArrayList<TextRange>();
            highlightRanges.add(pair.first.getTextRange());
            if (pair.second != null) {
                highlightRanges.add(pair.second.getTextRange());
            }

            if (!CommonRefactoringUtil.checkReadOnlyStatus(project, tag)) {
                return;
            }

            myHighlighters = new ArrayList<RangeHighlighter>();

            CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    final int offset = myEditor.getCaretModel().getOffset();
                    myEditor.getCaretModel().moveToOffset(tag.getTextOffset());

                    final Template t = buildTemplate(tag, pair);
                    TemplateManager.getInstance(project).startTemplate(
                        myEditor,
                        t,
                        new TemplateEditingAdapter() {
                            public void templateFinished(final Template template, boolean brokenOff) {
                                finish();
                            }

                            public void templateCancelled(final Template template) {
                                finish();
                            }
                        },
                        (variableName, value) -> value.length() == 0 || value.charAt(value.length() - 1) != ' '
                    );

                    // restore old offset
                    myEditor.getCaretModel().moveToOffset(offset);

                    addHighlights(highlightRanges, myEditor, myHighlighters);
                }),
                RefactoringBundle.message("rename.title"),
                null
            );
        }
    }

    private void finish() {
        ourRenamersStack.pop();

        if (myHighlighters != null) {
            final HighlightManager highlightManager = HighlightManager.getInstance(myEditor.getProject());
            for (final RangeHighlighter highlighter : myHighlighters) {
                highlightManager.removeSegmentHighlighter(myEditor, highlighter);
            }
        }
    }

    private Pair<ASTNode, ASTNode> getNamePair(@Nonnull final XmlTag tag) {
        final int offset = myEditor.getCaretModel().getOffset();

        final ASTNode node = tag.getNode();
        assert node != null;

        final ASTNode startTagName = XmlChildRole.START_TAG_NAME_FINDER.findChild(node);
        if (startTagName == null) {
            return null;
        }

        final ASTNode endTagName = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(node);

        final ASTNode selected = (endTagName == null ||
            startTagName.getTextRange().contains(offset) ||
            startTagName.getTextRange().contains(offset - 1))
            ? startTagName
            : endTagName;
        final ASTNode other = (selected == startTagName) ? endTagName : startTagName;

        return new Pair<ASTNode, ASTNode>(selected, other);
    }

    private static Template buildTemplate(@Nonnull final XmlTag tag, @Nonnull final Pair<ASTNode, ASTNode> pair) {
        final TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(tag);

        final ASTNode selected = pair.first;
        final ASTNode other = pair.second;

        builder.replaceElement(selected.getPsi(), PRIMARY_VARIABLE_NAME, new EmptyExpression() {
            public Result calculateQuickResult(final ExpressionContext context) {
                return new TextResult(selected.getText());
            }

            public Result calculateResult(final ExpressionContext context) {
                return new TextResult(selected.getText());
            }
        }, true);

        if (other != null) {
            builder.replaceElement(other.getPsi(), OTHER_VARIABLE_NAME, PRIMARY_VARIABLE_NAME, false);
        }

        return builder.buildInlineTemplate();
    }

    private static void addHighlights(List<TextRange> ranges, Editor editor, ArrayList<RangeHighlighter> highlighters) {
        EditorColorsManager colorsManager = EditorColorsManager.getInstance();
        final TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);

        final HighlightManager highlightManager = HighlightManager.getInstance(editor.getProject());
        for (final TextRange range : ranges) {
            highlightManager.addOccurrenceHighlight(
                editor,
                range.getStartOffset(),
                range.getEndOffset(),
                attributes,
                0,
                highlighters,
                null
            );
        }

        for (RangeHighlighter highlighter : highlighters) {
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
        }
    }
}
