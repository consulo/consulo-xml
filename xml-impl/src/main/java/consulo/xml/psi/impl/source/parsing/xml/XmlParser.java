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
 * @author max
 */
package consulo.xml.psi.impl.source.parsing.xml;

import consulo.language.ast.*;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.parser.ReparseMergeCustomComparator;
import consulo.language.version.LanguageVersion;
import consulo.util.lang.ThreeState;
import consulo.util.lang.ref.Ref;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

public class XmlParser implements PsiParser {
  // tries to match an old and new XmlTag by name
  private static final ReparseMergeCustomComparator REPARSE_XML_TAG_BY_NAME = (oldNode, newNode, structure) -> {
    if (oldNode instanceof XmlTag && newNode.getTokenType() == XmlElementType.XML_TAG) {
      String oldName = ((XmlTag) oldNode).getName();
      Ref<LighterASTNode[]> childrenRef = Ref.create(null);
      int count = structure.getChildren(newNode, childrenRef);
      if (count < 3) {
        return ThreeState.UNSURE;
      }
      LighterASTNode[] children = childrenRef.get();
      if (children[0].getTokenType() != XmlTokenType.XML_START_TAG_START) {
        return ThreeState.UNSURE;
      }
      if (children[1].getTokenType() != XmlTokenType.XML_NAME) {
        return ThreeState.UNSURE;
      }
      if (children[2].getTokenType() != XmlTokenType.XML_TAG_END) {
        return ThreeState.UNSURE;
      }
      LighterASTTokenNode name = (LighterASTTokenNode) children[1];
      CharSequence newName = name.getText();
      if (!oldName.equals(newName)) {
        return ThreeState.NO;
      }
    }

    return ThreeState.UNSURE;
  };

  @Nonnull
  public ASTNode parse(@Nonnull final IElementType root, @Nonnull final PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
    builder.enforceCommentTokens(TokenSet.EMPTY);
    builder.setReparseMergeCustomComparator(REPARSE_XML_TAG_BY_NAME);
    builder.registerWhitespaceToken(XmlTokenType.XML_REAL_WHITE_SPACE);

    final PsiBuilder.Marker file = builder.mark();
    new XmlParsing(builder).parseDocument();
    file.done(root);
    return builder.getTreeBuilt();
  }
}
