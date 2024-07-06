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
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.annotation.Annotation;
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
  public void annotate(final PsiElement psiElement, final AnnotationHolder holder) {
    if (psiElement instanceof XmlToken) {
      final PsiElement parent = psiElement.getParent();
      if (parent instanceof XmlTag) {
        final XmlTag tag = (XmlTag)parent;
        final XmlToken start = XmlTagUtil.getStartTagNameElement(tag);
        XmlToken endTagName = XmlTagUtil.getEndTagNameElement(tag);
        if (endTagName != null && !(tag instanceof HtmlTag) && !tag.getName().equals(endTagName.getText())) {
          registerProblem(holder, tag, start, endTagName);
        }
        else if (endTagName == null && !(tag instanceof HtmlTag && HtmlUtil.isSingleHtmlTag(tag.getName()))) {
          final PsiErrorElement errorElement = PsiTreeUtil.getChildOfType(tag, PsiErrorElement.class);
          endTagName = findEndTagName(errorElement);
          if (endTagName != null) {
            registerProblem(holder, tag, start, endTagName);
          }
        }
      }
      else if (parent instanceof PsiErrorElement) {
        if (XmlTokenType.XML_NAME == ((XmlToken)psiElement).getTokenType()) {
          final PsiFile psiFile = psiElement.getContainingFile();

          if (psiFile != null && (HTMLLanguage.INSTANCE == psiFile.getViewProvider().getBaseLanguage() || HTMLLanguage.INSTANCE == parent.getLanguage())) {
            final LocalizeValue message = XmlErrorLocalize.xmlParsingClosingTagMatchesNothing();

            if (message.equals(((PsiErrorElement)parent).getErrorDescription()) &&
                psiFile.getContext() == null
               ) {
              final Annotation annotation = holder.createWarningAnnotation(parent, message.get());
              annotation.registerFix(new RemoveExtraClosingTagIntentionAction());
            }
          }
        }
      }
    }
  }

  private static void registerProblem(
    @Nonnull final AnnotationHolder holder,
    @Nonnull final XmlTag tag,
    @Nullable final XmlToken start,
    @Nonnull final XmlToken end
  ) {
    PsiElement context = tag.getContainingFile().getContext();
    if (context != null) {
      ParserDefinition parserDefinition = ParserDefinition.forLanguage(context.getLanguage());
      if (parserDefinition != null) {
        ASTNode contextNode = context.getNode();
        if (contextNode != null && contextNode.getChildren(parserDefinition.getStringLiteralElements(context.getLanguageVersion())) != null) {
          // TODO: we should check for concatenations here
          return;
        }
      }
    }
    final String tagName = (tag instanceof HtmlTag) ? tag.getName().toLowerCase() : tag.getName();
    final String endTokenText = (tag instanceof HtmlTag) ? end.getText().toLowerCase() : end.getText();

    final RenameTagBeginOrEndIntentionAction renameEndAction = new RenameTagBeginOrEndIntentionAction(tagName, endTokenText, false);
    final RenameTagBeginOrEndIntentionAction renameStartAction = new RenameTagBeginOrEndIntentionAction(endTokenText, tagName, true);

    if (start != null) {
      holder.newAnnotation(HighlightSeverity.ERROR, XmlErrorLocalize.tagHasWrongClosingTagName().get())
        .range(start)
        .withFix(renameEndAction)
        .withFix(renameStartAction)
        .create();
    }

    holder.newAnnotation(HighlightSeverity.ERROR, XmlErrorLocalize.tagHasWrongClosingTagName().get())
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
