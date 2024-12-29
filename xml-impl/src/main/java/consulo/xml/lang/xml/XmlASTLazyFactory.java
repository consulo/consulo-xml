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
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.impl.ast.ASTLazyFactory;
import consulo.language.impl.ast.LazyParseableElement;
import consulo.language.impl.psi.template.TemplateDataElementType;
import consulo.xml.psi.impl.source.tree.HtmlFileElement;
import consulo.xml.psi.impl.source.tree.XmlFileElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.xml.psi.xml.XmlElementType.*;

/**
 * @author VISTALL
 * @since 2:39/02.04.13
 */
@ExtensionImpl
public class XmlASTLazyFactory implements ASTLazyFactory {
  @Nonnull
  @Override
  public LazyParseableElement createLazy(ILazyParseableElementType type, CharSequence text) {
    if (type == XML_FILE) {
      return new XmlFileElement(type, text);
    } else if (type == DTD_FILE) {
      return new XmlFileElement(type, text);
    } else if (type == XHTML_FILE) {
      return new XmlFileElement(type, text);
    } else if (type == HTML_FILE) {
      return new HtmlFileElement(text);
    } else if (type instanceof TemplateDataElementType) {
      return new XmlFileElement(type, text);
    }
    return null;
  }

  @Override
  public boolean test(@Nullable IElementType input) {
    return input == XML_FILE || input == DTD_FILE || input == XHTML_FILE || input == HTML_FILE || input instanceof TemplateDataElementType;
  }
}
