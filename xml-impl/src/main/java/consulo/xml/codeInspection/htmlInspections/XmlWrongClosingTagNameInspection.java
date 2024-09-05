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

package consulo.xml.codeInspection.htmlInspections;

import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlTagUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author spleaner
 */
public class XmlWrongClosingTagNameInspection implements Annotator {

  @Override
  @RequiredReadAction
  public void annotate(@Nonnull PsiElement psiElement, @Nonnull AnnotationHolder holder) {
    if (psiElement instanceof XmlToken) {
      PsiElement parent = psiElement.getParent();
      if (parent instanceof XmlTag tag) {
        XmlToken start = XmlTagUtil.getStartTagNameElement(tag);
        XmlToken endTagName = XmlTagUtil.getEndTagNameElement(tag);
        if (start == psiElement) {
          if (endTagName != null && !(tag instanceof HtmlTag) && !tag.getName().equals(endTagName.getText())) {
            registerProblemStart(holder, tag, start, endTagName);
          }
          else if (endTagName == null && !(tag instanceof HtmlTag && HtmlUtil.isSingleHtmlTag(tag, true))) {
            PsiErrorElement errorElement = PsiTreeUtil.getChildOfType(tag, PsiErrorElement.class);
            endTagName = findEndTagName(errorElement);
            if (endTagName != null) {
              registerProblemStart(holder, tag, start, endTagName);
            }
          }
        }
        else if (endTagName == psiElement) {
          if (!(tag instanceof HtmlTag) && !tag.getName().equals(endTagName.getText())) {
            registerProblemEnd(holder, tag, endTagName);
          }
        }
      }
    }
    else if (psiElement instanceof PsiErrorElement) {
      PsiElement[] children = psiElement.getChildren();
      for (PsiElement token : children) {
        if (token instanceof XmlToken && XmlTokenType.XML_NAME == ((XmlToken)token).getTokenType()) {
          PsiFile psiFile = holder.getCurrentAnnotationSession().getFile();

          if (HTMLLanguage.INSTANCE == psiFile.getViewProvider().getBaseLanguage() || HTMLLanguage.INSTANCE == psiElement.getLanguage()) {
            LocalizeValue message = XmlErrorLocalize.xmlParsingClosingTagMatchesNothing();

            if (message.equals(((PsiErrorElement)psiElement).getErrorDescriptionValue()) && psiFile.getContext() == null) {
              holder.newAnnotation(HighlightSeverity.WARNING, message)
                    .range(psiElement)
                    .withFix(new RemoveExtraClosingTagIntentionAction())
                    .create();
            }
          }
        }
      }
    }
  }

  @RequiredReadAction
  private static void registerProblemStart(
    @Nonnull final AnnotationHolder holder,
    @Nonnull final XmlTag tag,
    @Nonnull final XmlToken start,
    @Nonnull final XmlToken end
  ) {
    PsiElement context = tag.getContainingFile().getContext();
    if (context != null) {
      ParserDefinition parserDefinition = ParserDefinition.forLanguage(context.getLanguage());
      if (parserDefinition != null) {
        ASTNode contextNode = context.getNode();
        if (contextNode != null) {
          // TODO: we should check for concatenations here
          return;
        }
      }
    }
    String tagName = tag.getName();
    String endTokenText = end.getText();

    RenameTagBeginOrEndIntentionAction renameEndAction = new RenameTagBeginOrEndIntentionAction(tagName, endTokenText, false);
    RenameTagBeginOrEndIntentionAction renameStartAction = new RenameTagBeginOrEndIntentionAction(endTokenText, tagName, true);

    holder.newAnnotation(HighlightSeverity.ERROR, XmlErrorLocalize.tagHasWrongClosingTagName())
      .range(start)
      .withFix(new RemoveExtraClosingTagIntentionAction())
      .withFix(renameEndAction)
      .withFix(renameStartAction)
      .create();
  }

  @RequiredReadAction
  private static void registerProblemEnd(@Nonnull AnnotationHolder holder,
                                         @Nonnull XmlTag tag,
                                         @Nonnull XmlToken end) {
    PsiElement context = tag.getContainingFile().getContext();
    if (context != null) {
      ParserDefinition parserDefinition = ParserDefinition.forLanguage(context.getLanguage());
      if (parserDefinition != null) {
        ASTNode contextNode = context.getNode();
        if (contextNode != null) {
          // TODO: we should check for concatenations here
          return;
        }
      }
    }
    String tagName = tag.getName();
    String endTokenText = end.getText();

    RenameTagBeginOrEndIntentionAction renameEndAction = new RenameTagBeginOrEndIntentionAction(tagName, endTokenText, false);
    RenameTagBeginOrEndIntentionAction renameStartAction = new RenameTagBeginOrEndIntentionAction(endTokenText, tagName, true);

    holder.newAnnotation(HighlightSeverity.ERROR, XmlErrorLocalize.tagHasWrongClosingTagName())
          .range(end)
          .withFix(new RemoveExtraClosingTagIntentionAction())
          .withFix(renameEndAction)
          .withFix(renameStartAction)
          .create();
  }

  @Nullable
  static XmlToken findEndTagName(@Nullable final PsiErrorElement element) {
    if (element == null) return null;

    final ASTNode astNode = element.getNode();
    if (astNode == null) return null;

    ASTNode current = astNode.getLastChildNode();
    ASTNode prev = current;

    while (current != null) {
      final IElementType elementType = prev.getElementType();

      if ((elementType == XmlElementType.XML_NAME || elementType == XmlElementType.XML_TAG_NAME) &&
          current.getElementType() == XmlElementType.XML_END_TAG_START) {
        return (XmlToken)prev.getPsi();
      }

      prev = current;
      current = current.getTreePrev();
    }

    return null;
  }
}
