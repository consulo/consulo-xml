/*
 * Copyright 2013 Consulo.org
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
package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.ASTCompositeFactory;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.psi.CompositePsiElement;
import consulo.language.impl.psi.template.TemplateDataElementType;
import consulo.xml.psi.impl.source.html.HtmlDocumentImpl;
import consulo.xml.psi.impl.source.html.HtmlTagImpl;
import consulo.xml.psi.impl.source.tree.XmlFileElement;
import consulo.xml.psi.impl.source.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.xml.psi.xml.XmlElementType.*;

/**
 * @author VISTALL
 * @since 2:41/02.04.13
 */
@ExtensionImpl
public class XmlASTCompositeFactory implements ASTCompositeFactory {
  @Nonnull
  @Override
  public CompositeElement createComposite(IElementType type) {
    if (type == XML_TAG) {
      return new XmlTagImpl();
    } else if (type == XML_CONDITIONAL_SECTION) {
      return new XmlConditionalSectionImpl();
    } else if (type == HTML_TAG) {
      return new HtmlTagImpl();
    } else if (type == XML_TEXT) {
      return new XmlTextImpl();
    } else if (type == XML_PROCESSING_INSTRUCTION) {
      return new XmlProcessingInstructionImpl();
    } else if (type == XML_DOCUMENT) {
      return new XmlDocumentImpl();
    } else if (type == HTML_DOCUMENT) {
      return new HtmlDocumentImpl();
    } else if (type == XML_PROLOG) {
      return new XmlPrologImpl();
    } else if (type == XML_DECL) {
      return new XmlDeclImpl();
    } else if (type == XML_ATTRIBUTE) {
      return new XmlAttributeImpl();
    } else if (type == XML_ATTRIBUTE_VALUE) {
      return new XmlAttributeValueImpl();
    } else if (type == XML_COMMENT) {
      return new XmlCommentImpl();
    } else if (type == XML_DOCTYPE) {
      return new XmlDoctypeImpl();
    } else if (type == XML_MARKUP_DECL) {
      return new XmlMarkupDeclImpl();
    } else if (type == XML_ELEMENT_DECL) {
      return new XmlElementDeclImpl();
    } else if (type == XML_ENTITY_DECL) {
      return new XmlEntityDeclImpl();
    } else if (type == XML_ATTLIST_DECL) {
      return new XmlAttlistDeclImpl();
    } else if (type == XML_ATTRIBUTE_DECL) {
      return new XmlAttributeDeclImpl();
    } else if (type == XML_NOTATION_DECL) {
      return new XmlNotationDeclImpl();
    } else if (type == XML_ELEMENT_CONTENT_SPEC) {
      return new XmlElementContentSpecImpl();
    } else if (type == XML_ELEMENT_CONTENT_GROUP) {
      return new XmlElementContentGroupImpl();
    } else if (type == XML_ENTITY_REF) {
      return new XmlEntityRefImpl();
    } else if (type == XML_ENUMERATED_TYPE) {
      return new XmlEnumeratedTypeImpl();
    } else if (type == XML_CDATA) {
      return new CompositePsiElement(XML_CDATA) {
      };
    } else if (type instanceof TemplateDataElementType) {
      return new XmlFileElement(type, null);
    }

    return null;
  }

  @Override
  public boolean test(@Nullable IElementType input) {
    return input == XML_TAG ||
        input == XML_CONDITIONAL_SECTION ||
        input == HTML_TAG ||
        input == XML_TEXT ||
        input == XML_PROCESSING_INSTRUCTION ||
        input == XML_DOCUMENT ||
        input == HTML_DOCUMENT ||
        input == XML_PROLOG ||
        input == XML_DECL ||
        input == XML_ATTRIBUTE ||
        input == XML_ATTRIBUTE_VALUE ||
        input == XML_COMMENT ||
        input == XML_DOCTYPE ||
        input == XML_MARKUP_DECL ||
        input == XML_ELEMENT_DECL ||
        input == XML_ENTITY_DECL ||
        input == XML_ATTLIST_DECL ||
        input == XML_ATTRIBUTE_DECL ||
        input == XML_NOTATION_DECL ||
        input == XML_ELEMENT_CONTENT_SPEC ||
        input == XML_ELEMENT_CONTENT_GROUP ||
        input == XML_ENTITY_REF ||
        input == XML_ENUMERATED_TYPE ||
        input == XML_CDATA ||
        input instanceof TemplateDataElementType;
  }
}
