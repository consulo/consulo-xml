/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.xml.actions.validate;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import consulo.component.ProcessCanceledException;
import com.intellij.xml.util.XmlResourceResolver;

public abstract class ErrorReporter {
    protected final Set<String> ourErrorsSet = new HashSet<String>();
    protected final ValidateXmlActionHandler myHandler;

    public ErrorReporter(ValidateXmlActionHandler handler) {
        myHandler = handler;
    }

    public abstract void processError(SAXParseException ex, ValidateXmlActionHandler.ProblemType warning) throws SAXException;

    public boolean filterValidationException(Exception ex) {
        if (ex instanceof ProcessCanceledException processCanceledException) throw processCanceledException;
        if (ex instanceof XmlResourceResolver.IgnoredResourceException ignoredResourceException) throw ignoredResourceException;

        if (ex instanceof FileNotFoundException ||
            ex instanceof MalformedURLException ||
            ex instanceof NoRouteToHostException ||
            ex instanceof SocketTimeoutException ||
            ex instanceof UnknownHostException ||
            ex instanceof ConnectException
        ) {
            // do not log problems caused by malformed and/or ignored external resources
            return true;
        }

        if (ex instanceof NullPointerException) {
            return true; // workaround for NPE at org.apache.xerces.impl.dtd.XMLDTDProcessor.checkDeclaredElements
        }

        return false;
    }

    public void startProcessing() {
        myHandler.doParse();
    }

    public boolean isStopOnUndeclaredResource() {
        return false;
    }

    public boolean isUniqueProblem(final SAXParseException e) {
        String error = myHandler.buildMessageString(e);
        if (ourErrorsSet.contains(error)) return false;
        ourErrorsSet.add(error);
        return true;
    }
}
