// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.xml.template.formatter;

import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Spacing;
import consulo.language.codeStyle.Wrap;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.formatter.xml.AnotherLanguageBlockWrapper;
import consulo.xml.psi.formatter.xml.ReadOnlyBlock;
import consulo.xml.psi.formatter.xml.XmlBlock;
import consulo.xml.psi.formatter.xml.XmlFormattingPolicy;
import consulo.xml.psi.formatter.xml.XmlTagBlock;
import consulo.language.psi.OuterLanguageElement;
import consulo.xml.language.psi.XmlElementType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static consulo.util.lang.ObjectUtil.notNull;

public class TemplateXmlBlock extends XmlBlock implements IndentInheritingBlock {
    private final AbstractXmlTemplateFormattingModelBuilder myBuilder;
    private Indent myIndent;

    private static final List<Block> EMPTY_BLOCK_LIST = new ArrayList<>();

    public TemplateXmlBlock(AbstractXmlTemplateFormattingModelBuilder builder,
                            ASTNode node,
                            Wrap wrap,
                            Alignment alignment,
                            XmlFormattingPolicy policy,
                            Indent indent,
                            TextRange textRange) {
        super(node, wrap, alignment, policy, indent, textRange);
        myBuilder = builder;
    }

    @Override
    protected XmlBlock createSimpleChild(ASTNode child, @Nullable Indent indent,
                                         @Nullable Wrap wrap, @Nullable Alignment alignment, @Nullable TextRange range) {
        return myBuilder.createXmlBlock(child, wrap, alignment, myXmlFormattingPolicy, indent, notNull(range, child.getTextRange()));
    }

    @Override
    protected XmlTagBlock createTagBlock(ASTNode child, Indent indent, Wrap wrap, Alignment alignment) {
        return myBuilder.createXmlTagBlock(child, wrap, alignment, myXmlFormattingPolicy, indent);
    }

    @Override
    protected Indent getChildDefaultIndent() {
        Indent indent = super.getChildDefaultIndent();
        if (indent == null) {
            indent = Indent.getNoneIndent();
        }
        return indent;
    }

    protected List<Block> buildChildrenNoMerge() {
        return super.buildChildren();
    }

    @Override
    protected List<Block> buildChildren() {
        try {
            List<Block> childBlocks = patchTopLevelChildBlocks(buildChildrenNoMerge());
            return myBuilder.mergeWithTemplateBlocks(childBlocks, myXmlFormattingPolicy.getSettings(), myXmlFormattingPolicy,
                                                     getChildDefaultIndent());
        }
        catch (FragmentedTemplateException fte) {
            return EMPTY_BLOCK_LIST;
        }
    }

    private List<Block> patchTopLevelChildBlocks(List<Block> originalBlocks) {
        if (myNode.getPsi() instanceof PsiFile) {
            List<Block> patchedBlocks = new ArrayList<>();
            for (Block block : originalBlocks) {
                if (block == originalBlocks.get(0) && block instanceof TemplateXmlBlock) {
                    patchedBlocks.addAll(((TemplateXmlBlock)block).buildChildrenNoMerge());
                }
                else {
                    patchedBlocks.add(block);
                }
            }
            return patchedBlocks;
        }
        else {
            return originalBlocks;
        }
    }

    @Override
    public void setIndent(Indent indent) {
        myIndent = indent;
    }

    @Override
    public Indent getIndent() {
        return myIndent != null ? myIndent : super.getIndent();
    }

    @Override
    public Spacing getSpacing(Block child1, Block child2) {
        if (child1 instanceof TemplateLanguageBlock && child2 instanceof TemplateLanguageBlock) {
            return ((TemplateLanguageBlock)child1).getSpacing((TemplateLanguageBlock)child2);
        }
        if (child1 instanceof TemplateLanguageBlock || child2 instanceof TemplateLanguageBlock) {
            return Spacing.createSpacing(0, Integer.MAX_VALUE, 0, true, myXmlFormattingPolicy.getKeepBlankLines());
        }
        return super.getSpacing(child1, child2);
    }

    public boolean isTextContainingTemplateElements() {
        if (isTextElement()) {
            for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                if (myBuilder.isOuterLanguageElement(child.getPsi())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected List<Block> splitComment() {
        if (myNode.getElementType() != XmlElementType.XML_COMMENT) {
            return EMPTY;
        }
        final ArrayList<Block> result = new ArrayList<>(3);
        ASTNode child = myNode.getFirstChildNode();
        boolean hasOuterLangElements = false;
        while (child != null) {
            if (child instanceof OuterLanguageElement) {
                hasOuterLangElements = true;
            }
            if (myBuilder.isOuterLanguageElement(child.getPsi())) {
                result.add(createTemplateFragmentWrapper(child));
            }
            else {
                result.add(new XmlBlock(child, null, null, myXmlFormattingPolicy, getChildIndent(), null, isPreserveSpace()));
            }
            child = child.getTreeNext();
        }
        return hasOuterLangElements ? result : EMPTY;
    }

    private AnotherLanguageBlockWrapper createTemplateFragmentWrapper(ASTNode child) {
        return new AnotherLanguageBlockWrapper(child, myXmlFormattingPolicy, new ReadOnlyBlock(child), null, child.getStartOffset(),
                                               child.getTextRange());
    }
}
