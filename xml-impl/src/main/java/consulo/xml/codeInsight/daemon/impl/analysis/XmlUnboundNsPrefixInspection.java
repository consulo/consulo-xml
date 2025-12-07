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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.XmlTagUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.highlight.HighlightLevelUtil;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlUnboundNsPrefixInspection extends XmlSuppressableInspectionTool {
    private static final String XML = "xml";

    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            private Boolean isXml;

            private boolean isXmlFile(XmlElement element) {
                if (isXml == null) {
                    PsiFile file = element.getContainingFile();
                    isXml = file instanceof XmlFile && !InjectedLanguageManager.getInstance(element.getProject()).isInjectedFragment(file);
                }
                return isXml;
            }

            @Override
            @RequiredReadAction
            public void visitXmlToken(XmlToken token) {
                if (isXmlFile(token) && token.getTokenType() == XmlTokenType.XML_NAME) {
                    PsiElement element = token.getPrevSibling();
                    while (element instanceof PsiWhiteSpace whiteSpace) {
                        element = whiteSpace.getPrevSibling();
                    }

                    if (element instanceof XmlToken xmlToken
                        && xmlToken.getTokenType() == XmlTokenType.XML_START_TAG_START
                        && element.getParent() instanceof XmlTag tag
                        && !(token.getNextSibling() instanceof OuterLanguageElement)) {
                        checkUnboundNamespacePrefix(tag, tag, tag.getNamespacePrefix(), token, holder, isOnTheFly);
                    }
                }
            }

            @Override
            @RequiredReadAction
            public void visitXmlAttribute(XmlAttribute attribute) {
                if (!isXmlFile(attribute)) {
                    return;
                }
                String namespace = attribute.getNamespace();
                if (attribute.isNamespaceDeclaration() || XmlUtil.XML_SCHEMA_INSTANCE_URI.equals(namespace)) {
                    return;
                }

                XmlTag tag = attribute.getParent();
                XmlElementDescriptor elementDescriptor = tag.getDescriptor();
                if (elementDescriptor == null || elementDescriptor instanceof AnyXmlElementDescriptor) {
                    return;
                }

                String name = attribute.getName();

                checkUnboundNamespacePrefix(attribute, tag, XmlUtil.findPrefixByQualifiedName(name), null, holder, isOnTheFly);
            }

            @Override
            @RequiredReadAction
            public void visitXmlAttributeValue(XmlAttributeValue value) {
                PsiReference[] references = value.getReferences();
                for (PsiReference reference : references) {
                    if (reference instanceof SchemaPrefixReference schemaPrefixReference
                        && !XML.equals(schemaPrefixReference.getNamespacePrefix()) && reference.resolve() == null) {
                        holder.newProblem(XmlErrorLocalize.unboundNamespace(schemaPrefixReference.getNamespacePrefix()))
                            .rangeByRef(reference)
                            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                            .create();
                    }
                }
            }
        };
    }

    @RequiredReadAction
    private static void checkUnboundNamespacePrefix(
        XmlElement element,
        XmlTag context,
        String namespacePrefix,
        XmlToken token,
        ProblemsHolder holder,
        boolean isOnTheFly
    ) {
        if (namespacePrefix.isEmpty() && (!(element instanceof XmlTag) || !(element.getParent() instanceof XmlDocument))
            || XML.equals(namespacePrefix)) {
            return;
        }

        String namespaceByPrefix = context.getNamespaceByPrefix(namespacePrefix);
        if (namespaceByPrefix.length() != 0) {
            return;
        }
        PsiFile psiFile = context.getContainingFile();
        if (!(psiFile instanceof XmlFile containingFile) || !HighlightLevelUtil.shouldInspect(containingFile)) {
            return;
        }

        XmlExtension extension = XmlExtension.getExtension(containingFile);
        if (extension.getPrefixDeclaration(context, namespacePrefix) != null) {
            return;
        }

        LocalizeValue message = isOnTheFly
            ? XmlErrorLocalize.unboundNamespace(namespacePrefix)
            : XmlErrorLocalize.unboundNamespaceNoParam();

        if (namespacePrefix.length() == 0) {
            XmlTag tag = (XmlTag) element;
            if (!XmlUtil.JSP_URI.equals(tag.getNamespace())) {
                reportTagProblem(
                    tag,
                    message,
                    null,
                    ProblemHighlightType.INFORMATION,
                    isOnTheFly ? new CreateNSDeclarationIntentionFix(context, namespacePrefix, token) : null,
                    holder
                );
            }
            return;
        }

        int prefixLength = namespacePrefix.length();
        TextRange range = new TextRange(0, prefixLength);
        HighlightInfoType infoType = extension.getHighlightInfoType(containingFile);
        ProblemHighlightType highlightType =
            infoType == HighlightInfoType.ERROR ? ProblemHighlightType.ERROR : ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
        if (element instanceof XmlTag tag) {
            CreateNSDeclarationIntentionFix fix =
                isOnTheFly ? new CreateNSDeclarationIntentionFix(context, namespacePrefix, token) : null;
            reportTagProblem(tag, message, range, highlightType, fix, holder);
        }
        else {
            holder.newProblem(message)
                .range(element, range)
                .highlightType(highlightType)
                .create();
        }
    }

    private static void reportTagProblem(
        XmlTag element,
        @Nonnull LocalizeValue message,
        TextRange range,
        ProblemHighlightType highlightType,
        CreateNSDeclarationIntentionFix fix,
        ProblemsHolder holder
    ) {
        XmlToken nameToken = XmlTagUtil.getStartTagNameElement(element);
        if (nameToken != null) {
            holder.newProblem(message)
                .range(nameToken, range)
                .withFix(fix)
                .highlightType(highlightType)
                .create();
        }
        nameToken = XmlTagUtil.getEndTagNameElement(element);
        if (nameToken != null) {
            holder.newProblem(message)
                .range(nameToken, range)
                .withFix(fix)
                .highlightType(highlightType)
                .create();
        }
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlLocalize.xmlInspectionsGroupName();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.xmlInspectionsUnboundPrefix();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "XmlUnboundNsPrefix";
    }
}
