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
package com.intellij.xml;

import consulo.xml.ide.highlighter.DTDFileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.ide.impl.idea.codeInsight.editorActions.TypedHandler;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class XmlFileTypeFactory extends FileTypeFactory {
  public void createFileTypes(@Nonnull final FileTypeConsumer consumer) {
    consumer.consume(HtmlFileType.INSTANCE, "html;htm;sht;shtm;shtml");
    consumer.consume(XHtmlFileType.INSTANCE, "xhtml");
    consumer.consume(DTDFileType.INSTANCE, "dtd;ent;mod;elt");

    consumer.consume(XmlFileType.INSTANCE, "xml;xsd;tld;xsl;jnlp;wsdl;jhm;ant;xul;xslt;rng;");
    TypedHandler.registerBaseLanguageQuoteHandler(XMLLanguage.class, TypedHandler.getQuoteHandlerForType(XmlFileType.INSTANCE));
  }
}
