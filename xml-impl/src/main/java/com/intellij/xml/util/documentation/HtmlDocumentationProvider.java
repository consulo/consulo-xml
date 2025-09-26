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
package com.intellij.xml.util.documentation;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.util.UserColorLookup;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.xml.lang.documentation.DocumentationUtil;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.xml.*;
import consulo.xml.util.ColorSampleLookupValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * @author maxim
 */
@ExtensionImpl
public class HtmlDocumentationProvider implements LanguageDocumentationProvider {
    private static DocumentationProvider ourStyleProvider;
    private static DocumentationProvider ourScriptProvider;

    public static final String ELEMENT_ELEMENT_NAME = "element";
    public static final String NBSP = ":&nbsp;";
    public static final String BR = "<br>";

    public static void registerStyleDocumentationProvider(DocumentationProvider documentationProvider) {
        ourStyleProvider = documentationProvider;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }

    @Nullable
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element instanceof SchemaPrefix schemaPrefix) {
            return schemaPrefix.getQuickNavigateInfo();
        }
        return null;
    }

    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        String result = getUrlForHtml(element, PsiTreeUtil.getParentOfType(originalElement, XmlTag.class, false));

        if (result == null && ourStyleProvider != null) {
            return ourStyleProvider.getUrlFor(element, originalElement);
        }

        return result != null ? Collections.singletonList(result) : null;
    }

    public static String getUrlForHtml(PsiElement element, XmlTag context) {
        final EntityDescriptor descriptor = findDocumentationDescriptor(element, context);

        return descriptor != null ? descriptor.getHelpRef() : null;
    }

    private static EntityDescriptor findDocumentationDescriptor(PsiElement element, XmlTag context) {
        boolean isTag = true;
        PsiElement nameElement = null;
        String key = null;

        if (element instanceof XmlElementDecl elementDecl) {
            nameElement = elementDecl.getNameElement();
        }
        else if (element instanceof XmlAttributeDecl attributeDecl) {
            nameElement = attributeDecl.getNameElement();
            isTag = false;
        }
        else if (element instanceof XmlTag tag) {
            final PsiMetaData metaData = tag.getMetaData();
            key = (metaData != null) ? metaData.getName() : null;
            isTag = tag.getLocalName().equals(ELEMENT_ELEMENT_NAME);
        }
        else if (element.getParent() instanceof XmlAttributeValue attributeValue) {
            isTag = false;
            key = ((XmlAttribute)attributeValue.getParent()).getName();
        }
        else if (element instanceof XmlAttributeValue) {
            isTag = false;
            final XmlAttribute xmlAttribute = (XmlAttribute)element.getParent();
            key = xmlAttribute.getName();
        }
        else if (element instanceof XmlAttribute attribute) {
            isTag = false;
            key = attribute.getName();
        }
        else if (element instanceof XmlElement) {
            nameElement = element;
            isTag = !(element.getParent() instanceof XmlAttribute);
        }
        else {
            nameElement = element;
            if (context == null) {
                isTag = false;
            }
            else {
                String text = element.getText();
                isTag = text != null && text.startsWith(context.getName());
            }
        }

        if (nameElement != null) {
            key = nameElement.getText();
        }

        key = (key != null) ? key.toLowerCase() : "";

        int dotIndex = key.indexOf('.');
        if (dotIndex > 0) {
            key = key.substring(0, dotIndex);
        }

        if (isTag) {
            return HtmlDescriptorsTable.getTagDescriptor(key);
        }
        else {
            return getDescriptor(key, context);
        }
    }

    private static HtmlAttributeDescriptor getDescriptor(String name, XmlTag context) {
        HtmlAttributeDescriptor attributeDescriptor = HtmlDescriptorsTable.getAttributeDescriptor(name);
        if (attributeDescriptor instanceof CompositeAttributeTagDescriptor compositeAttributeTagDescriptor) {
            return compositeAttributeTagDescriptor.findHtmlAttributeInContext(context);
        }

        return attributeDescriptor;
    }

    public String generateDoc(PsiElement element, PsiElement originalElement) {
        final XmlTag tag = PsiTreeUtil.getParentOfType(originalElement, XmlTag.class, false);
        String result = generateDocForHtml(element, false, tag, originalElement);

        if (result == null && ourStyleProvider != null) {
            result = ourStyleProvider.generateDoc(element, originalElement);
        }

        if (result == null && ourScriptProvider != null) {
            result = ourScriptProvider.generateDoc(element, originalElement);
        }

        if (result == null && element instanceof XmlAttributeValue) {
            result = generateDocForHtml(element.getParent(), false, tag, originalElement);
        }

        return result;
    }

    public String generateDocForHtml(PsiElement element) {
        return generateDocForHtml(element, true, null, null);
    }

    protected String generateDocForHtml(PsiElement element, boolean ommitHtmlSpecifics, XmlTag context, PsiElement originalElement) {
        final EntityDescriptor descriptor = findDocumentationDescriptor(element, context);

        if (descriptor != null) {
            return generateJavaDoc(descriptor, ommitHtmlSpecifics, originalElement);
        }
        if (element instanceof XmlEntityDecl entityDecl) {
            return new XmlDocumentationProvider().findDocRightAfterElement(element, entityDecl.getName());
        }
        return null;
    }

    private static String generateJavaDoc(EntityDescriptor descriptor, boolean ommitHtmlSpecifics, PsiElement element) {
        StringBuilder buf = new StringBuilder();
        final boolean istag = descriptor instanceof HtmlTagDescriptor;

        if (istag) {
            DocumentationUtil.formatEntityName(XmlLocalize.xmlJavadocTagNameMessage().get(), descriptor.getName(), buf);
        }
        else {
            DocumentationUtil.formatEntityName(XmlLocalize.xmlJavadocAttributeNameMessage().get(), descriptor.getName(), buf);
        }

        buf.append(XmlLocalize.xmlJavadocDescriptionMessage()).append(NBSP).append(descriptor.getDescription()).append(BR);

        if (istag) {
            final HtmlTagDescriptor tagDescriptor = (HtmlTagDescriptor)descriptor;

            if (!ommitHtmlSpecifics) {
                boolean hasStartTag = tagDescriptor.isHasStartTag();
                if (!hasStartTag) {
                    buf.append(XmlLocalize.xmlJavadocStartTagCouldBeOmittedMessage()).append(BR);
                }
                if (!tagDescriptor.isEmpty() && !tagDescriptor.isHasEndTag()) {
                    buf.append(XmlLocalize.xmlJavadocEndTagCouldBeOmittedMessage()).append(BR);
                }
            }

            if (tagDescriptor.isEmpty()) {
                buf.append(XmlLocalize.xmlJavadocIsEmptyMessage()).append(BR);
            }
        }
        else {
            final HtmlAttributeDescriptor attributeDescriptor = (HtmlAttributeDescriptor)descriptor;

            buf.append(XmlLocalize.xmlJavadocAttrTypeMessage(attributeDescriptor.getType())).append(BR);
            if (!attributeDescriptor.isHasDefaultValue()) {
                buf.append(XmlLocalize.xmlJavadocAttrDefaultRequiredMessage()).append(BR);
            }
        }

        char dtdId = descriptor.getDtd();
        boolean deprecated = dtdId == HtmlTagDescriptor.LOOSE_DTD;
        if (deprecated) {
            buf.append(XmlLocalize.xmlJavadocDeprecatedMessage(deprecated)).append(BR);
        }

        if (dtdId == HtmlTagDescriptor.LOOSE_DTD) {
            buf.append(XmlLocalize.xmlJavadocDefinedInLooseDtdMessage());
        }
        else if (dtdId == HtmlTagDescriptor.FRAME_DTD) {
            buf.append(XmlLocalize.xmlJavadocDefinedInFramesetDtdMessage());
        }
        else {
            buf.append(XmlLocalize.xmlJavadocDefinedInAnyDtdMessage());
        }

        if (!istag) {
            addColorPreviewAndCodeToLookup(element, buf);
        }

        if (element != null) {
            buf.append(XmlDocumentationProvider.generateHtmlAdditionalDocTemplate(element));
        }

        return buf.toString();
    }

    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        PsiElement result = createNavigationElementHTML(psiManager, object.toString(), element);

        if (result == null && ourStyleProvider != null) {
            result = ourStyleProvider.getDocumentationElementForLookupItem(psiManager, object, element);
        }
        if (result == null && ourScriptProvider != null) {
            result = ourScriptProvider.getDocumentationElementForLookupItem(psiManager, object, element);
        }
        if (result == null && object instanceof String && element != null) {
            result = XmlDocumentationProvider.findDeclWithName((String)object, element);
        }
        return result;
    }

    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        PsiElement result = createNavigationElementHTML(psiManager, link, context);

        if (result == null && ourStyleProvider != null) {
            result = ourStyleProvider.getDocumentationElementForLink(psiManager, link, context);
        }
        if (result == null && ourScriptProvider != null) {
            result = ourScriptProvider.getDocumentationElementForLink(psiManager, link, context);
        }
        return result;
    }

    public PsiElement createNavigationElementHTML(PsiManager psiManager, String text, PsiElement context) {
        String key = text.toLowerCase();
        final HtmlTagDescriptor descriptor = HtmlDescriptorsTable.getTagDescriptor(key);

        if (descriptor != null && !isAttributeContext(context)) {
            try {
                final XmlTag tagFromText = XmlElementFactory.getInstance(psiManager.getProject())
                    .createTagFromText("<" + key + " xmlns=\"" + XmlUtil.XHTML_URI + "\"/>");
                final XmlElementDescriptor tagDescriptor = tagFromText.getDescriptor();
                return tagDescriptor != null ? tagDescriptor.getDeclaration() : null;
            }
            catch (IncorrectOperationException ignore) {
            }
        }
        else {
            XmlTag tagContext = findTagContext(context);
            HtmlAttributeDescriptor myAttributeDescriptor = getDescriptor(key, tagContext);

            if (myAttributeDescriptor != null && tagContext != null) {
                XmlElementDescriptor tagDescriptor = tagContext.getDescriptor();
                XmlAttributeDescriptor attributeDescriptor =
                    tagDescriptor != null ? tagDescriptor.getAttributeDescriptor(text, tagContext) : null;

                return (attributeDescriptor != null) ? attributeDescriptor.getDeclaration() : null;
            }
        }
        return null;
    }

    protected boolean isAttributeContext(PsiElement context) {
        if (context instanceof XmlAttribute) {
            return true;
        }

        if (context instanceof PsiWhiteSpace) {
            PsiElement prevSibling = context.getPrevSibling();
            if (prevSibling instanceof XmlAttribute) {
                return true;
            }
        }

        return false;
    }

    protected XmlTag findTagContext(PsiElement context) {
        if (context instanceof PsiWhiteSpace && context.getPrevSibling() instanceof XmlTag tag) {
            return tag;
        }

        return PsiTreeUtil.getParentOfType(context, XmlTag.class, false);
    }

    public static void addColorPreviewAndCodeToLookup(final PsiElement currentElement, final StringBuilder buf) {
        final Color colorFromElement = UserColorLookup.getColorFromElement(currentElement);

        if (colorFromElement != null) {
            ColorSampleLookupValue.addColorPreviewAndCodeToLookup(colorFromElement, buf);
        }
    }

    public static void registerScriptDocumentationProvider(final DocumentationProvider provider) {
        ourScriptProvider = provider;
    }
}
