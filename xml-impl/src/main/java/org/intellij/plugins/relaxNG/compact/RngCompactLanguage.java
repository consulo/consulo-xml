/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.compact;

import consulo.language.Language;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.ast.IElementType;
import consulo.language.BracePair;
import consulo.language.Commenter;
import consulo.language.PairedBraceMatcher;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.project.Project;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElement;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 01.08.2007
*/
public class RngCompactLanguage extends Language {
  public static final String ID = "RELAX-NG";

  public static final RngCompactLanguage INSTANCE = new RngCompactLanguage();

  private RngCompactLanguage() {
    super(ID, "application/relax-ng-compact-syntax");
  }

  public static class MyCommenter implements Commenter {
    @Override
    @Nullable
    public String getLineCommentPrefix() {
      return "#";
    }

    @Override
    @Nullable
    public String getBlockCommentPrefix() {
      return null;
    }

    @Override
    @Nullable
    public String getBlockCommentSuffix() {
      return null;
    }

    @Override
    public String getCommentedBlockCommentPrefix() {
      return null;
    }

    @Override
    public String getCommentedBlockCommentSuffix() {
      return null;
    }
  }

  public static class MyPairedBraceMatcher implements PairedBraceMatcher {
    private BracePair[] myBracePairs;

    @Override
    public BracePair[] getPairs() {
      if (myBracePairs == null) {
        myBracePairs = new BracePair[]{
                new BracePair(RncTokenTypes.LBRACE, RncTokenTypes.RBRACE, true),
                new BracePair(RncTokenTypes.LPAREN, RncTokenTypes.RPAREN, false),
                new BracePair(RncTokenTypes.LBRACKET, RncTokenTypes.RBRACKET, false),
        };
      }
      return myBracePairs;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@Nonnull IElementType lbraceType, @Nullable IElementType contextType) {
      return false;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
      // TODO
      return openingBraceOffset;
    }
  }

  public static class MyNamesValidator implements NamesValidator {
    @Override
    public boolean isKeyword(@Nonnull String name, Project project) {
      return RenameUtil.isKeyword(name);
    }

    @Override
    public boolean isIdentifier(@Nonnull String name, Project project) {
      return RenameUtil.isIdentifier(name);
    }
  }

  public static class MyDocumentationProvider implements DocumentationProvider {
    @Override
    @Nullable
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
      return null;
    }

    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
      return null;
    }

    @Override
    @Nullable
    public String generateDoc(PsiElement element, PsiElement originalElement) {
      if (element instanceof RncElement) {
        PsiElement comment = element.getPrevSibling();
        while (comment instanceof PsiWhiteSpace) {
          comment = comment.getPrevSibling();
        }
        if (comment instanceof PsiComment) {
          final StringBuilder sb = new StringBuilder();
          do {
            sb.insert(0, EscapeUtil.unescapeText(comment).replaceAll("\n?##?", "") + "<br>");
            comment = comment.getPrevSibling();
          } while (comment instanceof PsiComment);

          if (element instanceof RncDefine) {
            sb.insert(0, "Define: <b>" + ((RncDefine)element).getName() + "</b><br>");
          }

          return sb.toString();
        }
      }
      return null;
    }

    @Override
    @Nullable
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
      return null;
    }

    @Override
    @Nullable
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
      return null;
    }
  }

  public static class MySyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
    @Override
    @Nonnull
    protected SyntaxHighlighter createHighlighter() {
      return new RncHighlighter();
    }
  }
}
