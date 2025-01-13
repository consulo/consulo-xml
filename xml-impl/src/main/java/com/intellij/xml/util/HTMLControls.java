/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xml.util;

import consulo.logging.Logger;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.StreamUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.Converter;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Document;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * @author Dennis.Ushakov
 */
public class HTMLControls {
    private static final Logger LOG = Logger.getInstance(HTMLControls.class);
    private static Control[] ourControls;

    public static Control[] getControls() {
        if (ourControls == null) {
            ourControls = loadControls();
        }
        return ourControls;
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    private static Control[] loadControls() {
        Document document;
        try {
            // use temporary bytes stream because otherwise inputStreamSkippingBOM will fail
            // on ZipFileInputStream used in jar files
            final InputStream stream = HTMLControls.class.getResourceAsStream("HtmlControls.xml");
            final byte[] bytes = StreamUtil.loadFromStream(stream);
            stream.close();
            final UnsyncByteArrayInputStream bytesStream = new UnsyncByteArrayInputStream(bytes);
            document = JDOMUtil.loadDocument(CharsetToolkit.inputStreamSkippingBOM(bytesStream));
            bytesStream.close();
        }
        catch (Exception e) {
            LOG.error(e);
            return new Control[0];
        }
        if (!document.getRootElement().getName().equals("htmlControls")) {
            LOG.error("HTMLControls storage is broken");
            return new Control[0];
        }
        return XmlSerializer.deserialize(document, Control[].class);
    }

    public enum TagState {
        REQUIRED,
        OPTIONAL,
        FORBIDDEN
    }

    @Tag("control")
    public static class Control {
        @Attribute("name")
        public String name;
        @Attribute(value = "startTag", converter = TagStateConverter.class)
        public TagState startTag;
        @Attribute(value = "endTag", converter = TagStateConverter.class)
        public TagState endTag;
        @Attribute("emptyAllowed")
        public boolean emptyAllowed;
        @Attribute(value = "autoClosedBy", converter = AutoCloseConverter.class)
        public Set<String> autoClosedBy = Collections.emptySet();
    }

    private static class TagStateConverter extends Converter<TagState> {
        @Nullable
        @Override
        public TagState fromString(@Nonnull String value) {
            return TagState.valueOf(value.toUpperCase(Locale.US));
        }

        @Nonnull
        @Override
        public String toString(@Nonnull TagState state) {
            return state.name().toLowerCase(Locale.US);
        }
    }

    private static class AutoCloseConverter extends Converter<Set<String>> {
        @Nullable
        @Override
        public Set<String> fromString(@Nonnull String value) {
            final Set<String> result = new HashSet<String>();
            for (String closingTag : StringUtil.split(value, ",")) {
                result.add(closingTag.trim().toLowerCase(Locale.US));
            }
            return result;
        }

        @Nonnull
        @Override
        public String toString(@Nonnull Set<String> o) {
            return StringUtil.join(o, ", ");
        }
    }
}
