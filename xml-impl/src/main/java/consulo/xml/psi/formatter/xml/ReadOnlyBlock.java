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
package consulo.xml.psi.formatter.xml;

import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Spacing;
import consulo.language.codeStyle.AbstractBlock;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ReadOnlyBlock extends AbstractBlock {
  private static final ArrayList<Block> EMPTY = new ArrayList<Block>();

  public ReadOnlyBlock(ASTNode node) {
    super(node, null, null);
  }

  public Spacing getSpacing(Block child1, @Nonnull Block child2) {
    return null;
  }

  public boolean isLeaf() {
    return true;
  }

  protected List<Block> buildChildren() {
    return EMPTY;
  }
}
