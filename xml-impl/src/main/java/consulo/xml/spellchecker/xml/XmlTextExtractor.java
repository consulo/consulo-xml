/*
 * Copyright 2013-2026 consulo.io
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
package consulo.xml.spellchecker.xml;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.NotNullLazyValue;
import consulo.document.util.TextRange;
import consulo.html.language.psi.HtmlTag;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.TextContent.Exclusion;
import consulo.language.spellcheker.text.TextContent.ExclusionKind;
import consulo.language.spellcheker.text.TextContentBuilder;
import consulo.language.spellcheker.text.TextExtractor;
import consulo.language.spellcheker.text.util.HtmlUtils;
import consulo.language.spellcheker.text.util.Text;
import consulo.util.collection.ContainerUtil;
import consulo.xml.language.psi.XmlDocument;
import consulo.xml.language.psi.XmlElementType;
import consulo.xml.language.psi.XmlEntityRef;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlText;
import consulo.xml.language.psi.XmlTokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static consulo.language.spellcheker.text.TextContent.TextDomain.COMMENTS;
import static consulo.language.spellcheker.text.TextContent.TextDomain.LITERALS;
import static consulo.language.spellcheker.text.TextContent.TextDomain.PLAIN_TEXT;

public abstract class XmlTextExtractor extends TextExtractor {
    private static final Pattern ESCAPE_EXCLUSIONS = Pattern.compile("\\\\[nt]");
    private static final TextContentBuilder builder = TextContentBuilder.FromPsi.removingIndents(" \t").removingLineSuffixes(" \t");

    private final Set<Class<? extends Language>> myEnabledDialects;

    @SafeVarargs
    protected XmlTextExtractor(Class<? extends Language>... enabledDialects) {
        myEnabledDialects = Set.of(enabledDialects);
    }

    protected Function<XmlTag, TagKind> tagClassifier(PsiElement context) {
        return _ -> TagKind.Unknown;
    }

    protected boolean shouldMaskEscapeSymbols() {
        return false;
    }

    @Override
    protected List<TextContent> buildTextContents(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        if (isText(element) && hasSuitableDialect(element)) {
            Function<XmlTag, TagKind> classifier = tagClassifier(element);
            PsiElement container = SyntaxTraverser.psiApi().parents(element)
                .find(e -> e instanceof XmlDocument || e instanceof XmlTag && classifier.apply((XmlTag) e) != TagKind.Inline);
            if (container != null) {
                Map<PsiElement, List<TextContent>> contentsInside = LanguageCachedValueUtil.getCachedValue(container, () ->
                    CachedValueProvider.Result.create(calcContents(container), container));
                return contentsInside.getOrDefault(element, List.of());
            }
        }

        IElementType type = PsiUtilCore.getElementType(element);
        if (type == XmlTokenType.XML_COMMENT_CHARACTERS && allowedDomains.contains(COMMENTS) && hasSuitableDialect(element)) {
            return ContainerUtil.createMaybeSingletonList(builder.build(element, COMMENTS));
        }

        if (type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN && allowedDomains.contains(LITERALS) && hasSuitableDialect(element)) {
            return ContainerUtil.createMaybeSingletonList(builder.build(element, LITERALS));
        }

        return List.of();
    }

    private Map<PsiElement, List<TextContent>> calcContents(PsiElement container) {
        if (container instanceof XmlTag && isNonText((XmlTag) container)) {
            return Collections.emptyMap();
        }

        Function<XmlTag, TagKind> classifier = tagClassifier(container);
        boolean unknownContainer = container instanceof XmlTag && classifier.apply((XmlTag) container) == TagKind.Unknown;

        NotNullLazyValue<TextContent> fullContent = NotNullLazyValue.createValue(() -> TextContent.psiFragment(PLAIN_TEXT, container));

        class Visitor extends PsiRecursiveElementWalkingVisitor {
            final Map<PsiElement, List<TextContent>> result = new HashMap<>();
            final List<PsiElement> group = new ArrayList<>();
            final Set<Integer> markupIndices = new HashSet<>();
            final Set<Integer> unknownIndices = new HashSet<>();
            final Set<XmlTag> inlineTags = new HashSet<>();
            boolean unknownBefore = unknownContainer;

            @Override
            public void visitElement(PsiElement each) {
                if (each instanceof XmlTag tag) {
                    TagKind kind = classifier.apply(tag);
                    if (kind != TagKind.Inline) {
                        boolean unknown = kind == TagKind.Unknown;
                        flushGroup(unknown);
                        unknownBefore = unknown;
                        return;
                    }

                    if (isInlineNonTextTag(tag.getName())) {
                        unknownIndices.add(group.size());
                        return; // skip the tag's contents
                    }

                    inlineTags.add(tag);
                    markupIndices.add(group.size());
                }
                if (each instanceof OuterLanguageElement) {
                    flushGroup(true);
                    unknownBefore = true;
                }
                if (each instanceof XmlEntityRef) {
                    if (HtmlUtils.isShyEntity(each.getText())) {
                        unknownIndices.add(group.size());
                    }
                    else {
                        flushGroup(true);
                        unknownBefore = true;
                    }
                }

                if (isText(each)) {
                    if (isCdata(each.getParent())) {
                        List<TextContent> contents = HtmlUtils.excludeHtml(
                            extractRange(each.getTextRange().shiftLeft(container.getTextRange().getStartOffset())));
                        if (!contents.isEmpty()) { // isolate CDATA into its own TextContent set for now; maybe glue to the surrounding texts later
                            flushGroup(false);
                            result.put(each, contents);
                            unknownBefore = false;
                        }
                    }
                    else {
                        group.add(each);
                    }
                }
                else if (PsiUtilCore.getElementType(each) == XmlTokenType.XML_CHAR_ENTITY_REF) {
                    if (HtmlUtils.isSpaceEntity(each.getText())) {
                        group.add(each);
                    }
                    else {
                        unknownIndices.add(group.size());
                    }
                }
                super.visitElement(each);
            }

            private TextContent extractRange(TextRange range) {
                TextContent full = fullContent.getValue();
                return full.excludeRange(new TextRange(range.getEndOffset(), full.length())).excludeRange(new TextRange(0, range.getStartOffset()));
            }

            @Override
            protected void elementFinished(PsiElement element) {
                super.elementFinished(element);
                if (inlineTags.contains(element)) {
                    markupIndices.add(group.size());
                }
            }

            private void flushGroup(boolean unknownAfter) {
                int containerStart = container.getTextRange().getStartOffset();
                List<TextContent> components = new ArrayList<>(group.size());
                int i = 0;
                while (i < group.size() && group.get(i) instanceof PsiWhiteSpace) {
                    i++;
                }
                while (i < group.size()) {
                    PsiElement e = group.get(i);
                    TextContent component = extractRange(e.getTextRange().shiftLeft(containerStart));
                    component = applyExclusions(i, component, markupIndices, ExclusionKind.markup);
                    component = applyExclusions(i, component, unknownIndices, ExclusionKind.unknown);
                    component = maskEscapeSymbols(component);
                    components.add(component);
                    i++;
                }
                unknownIndices.clear();
                markupIndices.clear();
                inlineTags.clear();
                TextContent content = TextContent.join(components);
                if (content != null) {
                    if (unknownBefore) {
                        content = content.markUnknown(TextRange.from(0, 0));
                    }
                    if (unknownAfter) {
                        content = content.markUnknown(TextRange.from(content.length(), 0));
                    }
                    content = HtmlUtils.inlineSpaceEntities(content.removeIndents(Set.of(' ', '\t')));
                    if (content != null) {
                        for (PsiElement e : group) {
                            result.put(e, List.of(content));
                        }
                    }
                }
                group.clear();
            }

            private static TextContent applyExclusions(int index, TextContent component, Set<Integer> indices, ExclusionKind kind) {
                TextContent result = component;
                if (indices.contains(index)) {
                    result = result.excludeRanges(List.of(new Exclusion(0, 0, kind)));
                }
                if (indices.contains(index + 1)) {
                    result = result.excludeRanges(List.of(new Exclusion(result.length(), result.length(), kind)));
                }
                return result;
            }
        }
        Visitor visitor = new Visitor();
        container.acceptChildren(visitor);
        visitor.flushGroup(unknownContainer);
        return visitor.result;
    }

    private TextContent maskEscapeSymbols(TextContent component) {
        if (!shouldMaskEscapeSymbols()) {
            return component;
        }
        return component.excludeRanges(ContainerUtil.map(Text.allOccurrences(ESCAPE_EXCLUSIONS, component), Exclusion::markUnknown));
    }

    private static boolean isText(PsiElement leaf) {
        PsiElement parent = leaf.getParent();
        if (!(parent instanceof XmlText) && !isCdata(parent) && !(parent instanceof XmlDocument)) {
            return false;
        }

        IElementType type = PsiUtilCore.getElementType(leaf);
        return type == XmlTokenType.XML_WHITE_SPACE || type == TokenType.WHITE_SPACE ||
            type == XmlTokenType.XML_DATA_CHARACTERS;
    }

    private static boolean isCdata(PsiElement element) {
        return PsiUtilCore.getElementType(element) == XmlElementType.XML_CDATA;
    }

    private boolean hasSuitableDialect(PsiElement element) {
        return myEnabledDialects.contains(element.getContainingFile().getLanguage().getClass());
    }

    private static boolean isNonText(XmlTag tag) {
        return tag instanceof HtmlTag && (isInlineNonTextTag(tag.getName()) || isBlockNonTextTag(tag.getName())) || isAuthorTag(tag.getName());
    }

    protected static boolean isBlockNonTextTag(String name) {
        return "pre".equals(name);
    }

    protected static boolean isInlineNonTextTag(String name) {
        return "code".equals(name) || "wbr".equals(name);
    }

    private static boolean isAuthorTag(String name) {
        return "author".equals(name) || "authors".equals(name);
    }

    protected enum TagKind {
        Block,
        Inline,
        Unknown
    }
}
