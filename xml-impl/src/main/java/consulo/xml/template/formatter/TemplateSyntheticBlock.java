/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.xml.template.formatter;

import consulo.language.codeStyle.ASTBlock;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Spacing;
import consulo.language.ast.ASTNode;
import consulo.xml.language.XMLLanguage;
import consulo.xml.psi.formatter.xml.SyntheticBlock;
import consulo.xml.psi.formatter.xml.XmlFormattingPolicy;

import java.util.List;

public class TemplateSyntheticBlock extends SyntheticBlock implements IndentInheritingBlock {
    private Indent myInheritedIndent;

    public TemplateSyntheticBlock(List<Block> subBlocks,
                                  Block parent,
                                  Indent indent,
                                  XmlFormattingPolicy policy,
                                  Indent childIndent) {
        super(subBlocks, parent, indent, policy, childIndent);
    }

    @Override
    public void setIndent(Indent indent) {
        myInheritedIndent = indent;
    }

    @Override
    public Indent getIndent() {
        return myInheritedIndent != null ? myInheritedIndent : super.getIndent();
    }

    @Override
    public Spacing getSpacing(Block child1, Block child2) {
        if (child1 != null && isXmlBlock(child1) != isXmlBlock(child2)) {
            if (shouldKeepWhiteSpacesInside()) {
                return Spacing.getReadOnlySpacing();
            }
            return Spacing.createSpacing(0, 1, 0, true, myXmlFormattingPolicy.getKeepBlankLines());
        }
        return super.getSpacing(child1, child2);
    }

    private static boolean isXmlBlock(Block block) {
        if (block instanceof TemplateXmlTagBlock || block instanceof TemplateXmlBlock) {
            return true;
        }
        if (block instanceof ASTBlock) {
            ASTNode node = ((ASTBlock)block).getNode();
            return node != null && node.getPsi().getLanguage().isKindOf(XMLLanguage.INSTANCE);
        }
        return false;
    }
}
