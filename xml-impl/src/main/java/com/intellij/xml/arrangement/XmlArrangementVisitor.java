package com.intellij.xml.arrangement;

import consulo.xml.psi.XmlElementVisitor;
import consulo.language.codeStyle.arrangement.DefaultArrangementEntry;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.util.collection.Stack;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.EntryType.XML_ATTRIBUTE;
import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.EntryType.XML_TAG;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlArrangementVisitor extends XmlElementVisitor {
    private final Stack<XmlElementArrangementEntry> myStack = new Stack<>();

    private final XmlArrangementParseInfo myInfo;
    private final Collection<TextRange> myRanges;

    public XmlArrangementVisitor(@Nonnull XmlArrangementParseInfo info, @Nonnull Collection<TextRange> ranges) {
        myInfo = info;
        myRanges = ranges;
    }

    @Override
    public void visitXmlFile(XmlFile file) {
        final XmlTag tag = file.getRootTag();

        if (tag != null) {
            tag.accept(this);
        }
    }

    @Override
    public void visitXmlTag(XmlTag tag) {
        final XmlElementArrangementEntry entry =
            createNewEntry(tag.getTextRange(), XML_TAG, null, true);
        processEntry(entry, tag);
    }

    @Override
    public void visitXmlAttribute(XmlAttribute attribute) {
        final String name = attribute.isNamespaceDeclaration() ? "" : attribute.getName();
        final XmlElementArrangementEntry entry =
            createNewEntry(attribute.getTextRange(), XML_ATTRIBUTE, name, true);
        processEntry(entry, null);
    }

    private void processEntry(@Nullable XmlElementArrangementEntry entry, @Nullable PsiElement nextElement) {
        if (entry == null || nextElement == null) {
            return;
        }
        myStack.push(entry);
        try {
            nextElement.acceptChildren(this);
        }
        finally {
            myStack.pop();
        }
    }

    @Nullable
    private XmlElementArrangementEntry createNewEntry(
        @Nonnull TextRange range,
        @Nonnull ArrangementSettingsToken type,
        @Nullable String name,
        boolean canBeMatched
    ) {
        if (!isWithinBounds(range)) {
            return null;
        }
        final DefaultArrangementEntry current = getCurrent();
        final XmlElementArrangementEntry entry =
            new XmlElementArrangementEntry(current, range, type, name, canBeMatched);

        if (current == null) {
            myInfo.addEntry(entry);
        }
        else {
            current.addChild(entry);
        }
        return entry;
    }

    @Nullable
    private DefaultArrangementEntry getCurrent() {
        return myStack.isEmpty() ? null : myStack.peek();
    }

    private boolean isWithinBounds(@Nonnull TextRange range) {
        for (TextRange textRange : myRanges) {
            if (textRange.intersects(range)) {
                return true;
            }
        }
        return false;
    }
}
