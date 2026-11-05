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

import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Wrap;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.xml.psi.formatter.xml.XmlBlock;
import consulo.xml.psi.formatter.xml.XmlFormattingPolicy;
import consulo.xml.psi.formatter.xml.XmlTagBlock;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.xml.util.HtmlUtil.SCRIPT_TAG_NAME;

public class TemplateXmlTagBlock extends XmlTagBlock implements IndentInheritingBlock {
    private final AbstractXmlTemplateFormattingModelBuilder myBuilder;
    private Indent myInheritedIndent;

    public TemplateXmlTagBlock(AbstractXmlTemplateFormattingModelBuilder builder,
                               ASTNode node,
                               Wrap wrap,
                               Alignment alignment,
                               XmlFormattingPolicy policy,
                               Indent indent) {
        super(node, wrap, alignment, policy, indent);
        myBuilder = builder;
    }

    @Override
    protected XmlTagBlock createTagBlock(ASTNode child, Indent indent, Wrap wrap, Alignment alignment) {
        return myBuilder.createXmlTagBlock(child, wrap, alignment, myXmlFormattingPolicy, indent);
    }

    @Override
    protected final Block createSyntheticBlock(ArrayList<Block> localResult, Indent childrenIndent) {
        try {
            List<Block> merged =
                myBuilder.mergeWithTemplateBlocks(localResult, myXmlFormattingPolicy.getSettings(), myXmlFormattingPolicy, childrenIndent);
            return myBuilder.createSyntheticBlock(merged, this, Indent.getNoneIndent(), myXmlFormattingPolicy, childrenIndent);
        }
        catch (FragmentedTemplateException e) {
            return myBuilder.createSyntheticBlock(localResult, this, Indent.getNoneIndent(), myXmlFormattingPolicy, childrenIndent);
        }
    }


    @Override
    protected XmlBlock createSimpleChild(ASTNode child, @Nullable Indent indent,
                                         @Nullable Wrap wrap, @Nullable Alignment alignment, @Nullable TextRange range) {
        return myBuilder.createXmlBlock(child, wrap, alignment, myXmlFormattingPolicy, indent, range);
    }

    @Override
    public void setIndent(Indent indent) {
        myInheritedIndent = indent;
    }

    @Override
    public Indent getIndent() {
        return myInheritedIndent == null ? super.getIndent() : myInheritedIndent;
    }

    @Override
    protected Indent getChildrenIndent() {
        return Indent.getIndent(myXmlFormattingPolicy.indentChildrenOf(getTag()) ? Indent.Type.NORMAL : Indent.Type.NONE, false, true);
    }

    boolean isScriptBlock() {
        var tag = getTag();
        return tag != null && tag.getLocalName().equalsIgnoreCase(SCRIPT_TAG_NAME);
    }
}
