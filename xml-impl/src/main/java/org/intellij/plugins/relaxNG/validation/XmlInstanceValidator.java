/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.validation;

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.ValidateProperty;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.xml.Validator;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 30.07.2007
 */
public class XmlInstanceValidator {
  private static final Logger LOG = Logger.getInstance("XmlInstanceValidator");

  private XmlInstanceValidator() {
  }

  public static void doValidation(@Nonnull final XmlDocument doc, final Validator.ValidationHost host, final XmlFile descriptorFile) {
    try {
      final Schema schema = RngParser.getCachedSchema(descriptorFile);
      if (schema == null) {
        // did not manage to get a compiled schema. no validation...
        return;
      }

      final ErrorHandler eh = MyErrorHandler.create(doc, host);
      if (eh == null) {
        return;
      }

      final PropertyMapBuilder builder = new PropertyMapBuilder();
      builder.put(ValidateProperty.ERROR_HANDLER, eh);

      final ContentHandler handler = schema.createValidator(builder.toPropertyMap()).getContentHandler();
      doc.accept(new Psi2SaxAdapter(handler));

    } catch (ProcessCanceledException e) {
      throw e;
    } catch (RuntimeException e) {
      LOG.error(e);
    } catch (Exception e) {
      LOG.info(e);
    }
  }

  private static class MyErrorHandler implements ErrorHandler {
    private final Validator.ValidationHost myHost;
    private final Document myDocument;
    private final PsiFile myFile;

    private MyErrorHandler(XmlDocument doc, Validator.ValidationHost host) {
      myHost = host;
      myFile = doc.getContainingFile();
      myDocument = PsiDocumentManager.getInstance(myFile.getProject()).getDocument(myFile);
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      RngSchemaValidator.handleError(exception, myFile, myDocument, new RngSchemaValidator.ValidationMessageConsumer() {
        @Override
        public void onMessage(PsiElement context, String message) {
          myHost.addMessage(context, message, Validator.ValidationHost.ErrorType.WARNING);
        }
      });
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      RngSchemaValidator.handleError(exception, myFile, myDocument, new RngSchemaValidator.ValidationMessageConsumer() {
        @Override
        public void onMessage(PsiElement context, String message) {
          myHost.addMessage(context, message, Validator.ValidationHost.ErrorType.ERROR);
        }
      });
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      RngSchemaValidator.handleError(exception, myFile, myDocument, new RngSchemaValidator.ValidationMessageConsumer() {
        @Override
        public void onMessage(PsiElement context, String message) {
          myHost.addMessage(context, message, Validator.ValidationHost.ErrorType.ERROR);
        }
      });
    }

    @Nullable
    public static ErrorHandler create(XmlDocument doc, Validator.ValidationHost host) {
      final XmlTag rootTag = doc.getRootTag();
      if (rootTag == null) {
        return null;
      }
      return new MyErrorHandler(doc, host);
    }
  }
}
