// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.xml.template.formatter;

import consulo.language.codeStyle.ASTBlock;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.BlockEx;
import consulo.language.codeStyle.ChildAttributes;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Spacing;
import consulo.language.codeStyle.Wrap;
import consulo.language.codeStyle.WrapType;
import consulo.language.codeStyle.template.BlockWithParent;
import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.AbstractBlock;
import consulo.xml.psi.formatter.xml.XmlFormattingPolicy;
import consulo.xml.language.psi.XmlDocument;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TemplateLanguageBlock extends AbstractBlock implements BlockEx, IndentInheritingBlock, BlockWithParent {
    private final CodeStyleSettings mySettings;
    private final AbstractXmlTemplateFormattingModelBuilder myBuilder;
    private final XmlFormattingPolicy myXmlFormattingPolicy;
    private @Nullable Indent myIndent;
    private BlockWithParent myParent;
    private boolean myContainsErrorElements = false;

    protected TemplateLanguageBlock(AbstractXmlTemplateFormattingModelBuilder builder,
                                    ASTNode node,
                                    @Nullable Wrap wrap,
                                    @Nullable Alignment alignment,
                                    CodeStyleSettings settings,
                                    XmlFormattingPolicy xmlFormattingPolicy,
                                    @Nullable Indent indent) {
        super(node, wrap, alignment);
        mySettings = settings;
        myBuilder = builder;
        myXmlFormattingPolicy = xmlFormattingPolicy;
        myIndent = indent;
    }

    protected List<Block> buildChildrenWithMerge() throws FragmentedTemplateException {
        final List<Block> markupBlocks = new ArrayList<>();
        List<PsiElement> markupElements = TemplateFormatUtil.findAllMarkupLanguageElementsInside(myNode.getPsi());
        if (markupElements.size() == 1 && markupElements.get(0) instanceof XmlDocument) {
            markupElements = getXmlDocumentChildren(markupElements.get(0));
        }
        boolean mergeFromMarkup = false;
        for (PsiElement markupElement : markupElements) {
            if (TemplateFormatUtil.isErrorElement(markupElement)) {
                throw new FragmentedTemplateException((PsiErrorElement)markupElement);
            }
            if (!(FormatterUtil.containsWhiteSpacesOnly(markupElement.getNode()))) {
                Block rootBlock = myBuilder
                    .createDataLanguageRootBlock(markupElement, markupElement.getLanguage(), mySettings, myXmlFormattingPolicy,
                                                 myNode.getPsi().getContainingFile(), getDefaultMarkupIndent());
                PsiElement parent = markupElement.getParent();
                if (!mergeFromMarkup) {
                    mergeFromMarkup = shouldMergeFromMarkup(rootBlock);
                }
                if (parent instanceof PsiFile ||
                    (rootBlock instanceof TemplateXmlBlock && ((TemplateXmlBlock)rootBlock).isTextContainingTemplateElements())) {
                    for (Block block : rootBlock.getSubBlocks()) {
                        if (containsErrorElement(block)) {
                            throw new FragmentedTemplateException();
                        }
                        markupBlocks.add(block);
                    }
                }
                else {
                    markupBlocks.add(rootBlock);
                }
            }
        }
        List<Block> result = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();
        while (child != null) {
            if (containsFatalError(child.getPsi()) && !markupBlocks.isEmpty()) {
                throw new FragmentedTemplateException();
            }
            if (!FormatterUtil.containsWhiteSpacesOnly(child) && child.getTextLength() > 0) {
                if (!myBuilder.isMarkupLanguageElement(child.getPsi())) {
                    addBlocksForNonMarkupChild(result, child);
                }
            }
            child = child.getTreeNext();
        }
        if (!markupBlocks.isEmpty()) {
            if (result.isEmpty()) {
                return markupBlocks;
            }
            if (mergeFromMarkup) {
                result = TemplateFormatUtil.mergeBlocks(markupBlocks, result, myNode.getTextRange());
            }
            else {
                result = TemplateFormatUtil.mergeBlocks(result, markupBlocks, myNode.getTextRange());
            }
            for (Block resultBlock : result) {
                ASTNode node = resultBlock instanceof ASTBlock ? ((ASTBlock)resultBlock).getNode() : null;
                if (node != null && resultBlock instanceof IndentInheritingBlock) {
                    ((IndentInheritingBlock)resultBlock).setIndent(getChildIndent(node));
                }
                if (resultBlock instanceof BlockWithParent) {
                    ((BlockWithParent)resultBlock).setParent(this);
                }
            }
        }
        return result;
    }

    /**
     * Checks that the given element contains fatal syntax errors which mean that the whole fragment is damaged and no reliable formatting
     * is possible, {@code false} by default but can be overridden in subclasses for specific logic/heuristics.
     *
     * @param element The element to check.
     * @return True if the fragment can't be reliably processed.
     */
    public boolean containsFatalError(PsiElement element) {
        return false;
    }

    @Override
    protected List<Block> buildChildren() {
        try {
            return buildChildrenWithMerge();
        }
        catch (FragmentedTemplateException e) {
            myContainsErrorElements = true;
            return AbstractBlock.EMPTY;
        }
    }

    private List<PsiElement> getXmlDocumentChildren(PsiElement xmlDocument) {
        List<PsiElement> children = new ArrayList<>();
        PsiElement child = xmlDocument.getFirstChild();
        while (child != null) {
            if (!myBuilder.isOuterLanguageElement(child)) {
                children.add(child);
            }
            child = child.getNextSibling();
        }
        return children;
    }

    private static boolean containsErrorElement(Block block) {
        if (block instanceof ASTBlock) {
            ASTNode node = ((ASTBlock)block).getNode();
            if (node != null) {
                return TemplateFormatUtil.isErrorElement(node.getPsi());
            }
        }
        return false;
    }

    protected void addBlocksForNonMarkupChild(List<Block> result, ASTNode child) {
        Block templateLanguageBlock = createTemplateLanguageBlock(child);
        if (templateLanguageBlock instanceof BlockWithParent) {
            ((BlockWithParent)templateLanguageBlock).setParent(this);
        }
        result.add(templateLanguageBlock);
    }

    protected Block createTemplateLanguageBlock(ASTNode child) {
        return myBuilder.createTemplateLanguageBlock(
                                                     child,
                                                     mySettings,
                                                     myXmlFormattingPolicy,
                                                     getChildIndent(child),
                                                     getChildAlignment(child),
                                                     getChildWrap(child)
                                                     );
    }

    protected AbstractXmlTemplateFormattingModelBuilder getBuilder() {
        return myBuilder;
    }

    /**
     * In practice, returning `true` from this method sometimes leads to an invalid block structure.
     * The same problem is observed in different template languages that use this class.
     * To avoid changing the existing behavior, the method isn't removed all together, 
     * but only its visibility is changed to protected to allow overriding it.
     * <p>
     * The primary user of this method will be the Scala Plugin Twirl Templates.
     *
     * @see <a href="https://youtrack.jetbrains.com/issue/SCL-25670/Play-Rewrite-Invalid-formatting-on-some-cases-with-script-element">SCL-25670</a>
     */
    protected boolean shouldMergeFromMarkup(Block block) {
        if (block instanceof TemplateXmlTagBlock) {
            return ((TemplateXmlTagBlock)block).isScriptBlock();
        }
        return false;
    }

    protected @Nullable Alignment getChildAlignment(ASTNode child) {
        return null;
    }

    @Override
    public @Nullable Indent getIndent() {
        return myIndent;
    }

    @Override
    protected final Indent getChildIndent() {
        return Indent.getNoneIndent();
    }

    protected abstract Indent getChildIndent(ASTNode node);

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null || myContainsErrorElements;
    }

    @Override
    public void setIndent(@Nullable Indent indent) {
        myIndent = indent;
    }

    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(Indent.getNormalIndent(), null);
    }

    protected Indent getDefaultMarkupIndent() {
        return Indent.getNormalIndent();
    }

    public CodeStyleSettings getSettings() {
        return mySettings;
    }

    @Override
    public BlockWithParent getParent() {
        return myParent;
    }

    @Override
    public void setParent(BlockWithParent newParent) {
        myParent = newParent;
    }

    protected @Nullable Wrap getChildWrap(ASTNode child) {
        return Wrap.createWrap(WrapType.NONE, false);
    }

    protected abstract Spacing getSpacing(TemplateLanguageBlock adjacentBlock);

    public XmlFormattingPolicy getXmlFormattingPolicy() {
        return myXmlFormattingPolicy;
    }

    public boolean containsErrorElements() {
        return myContainsErrorElements;
    }

    @Override
    public @Nullable Language getLanguage() {
        return myNode.getPsi().getLanguage();
    }
}
