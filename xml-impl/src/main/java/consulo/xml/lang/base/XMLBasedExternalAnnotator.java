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
package consulo.xml.lang.base;

import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.util.XmlTagUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.ExternalAnnotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.xml.Validator;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ven
 */
public abstract class XMLBasedExternalAnnotator
    extends ExternalAnnotator<XMLBasedExternalAnnotator.MyHost, XMLBasedExternalAnnotator.MyHost> {
    @Nullable
    @Override
    public MyHost collectInformation(@Nonnull PsiFile file) {
        if (!(file instanceof XmlFile xmlFile)) {
            return null;
        }
        XmlDocument document = xmlFile.getDocument();
        if (document == null) {
            return null;
        }
        XmlTag rootTag = document.getRootTag();
        XmlNSDescriptor nsDescriptor = rootTag == null ? null : rootTag.getNSDescriptor(rootTag.getNamespace(), false);

        if (nsDescriptor instanceof Validator validator) {
            //noinspection unchecked
            MyHost host = new MyHost();
            ((Validator<XmlDocument>) validator).validate(document, host);
            return host;
        }
        return null;
    }

    @Nullable
    @Override
    public MyHost doAnnotate(MyHost collectedInfo) {
        return collectedInfo;
    }

    @Override
    @RequiredReadAction
    public void apply(@Nonnull PsiFile file, MyHost annotationResult, @Nonnull AnnotationHolder holder) {
        annotationResult.apply(holder);
    }

    private static void appendFixes(AnnotationBuilder builder, IntentionAction... actions) {
        if (actions == null) {
            return;
        }
        for (IntentionAction action : actions) {
            builder.newFix(action);
        }
    }

    static class MyHost implements Validator.ValidationHost {
        private record Message(PsiElement context, @Nonnull LocalizeValue message, @Nonnull ErrorType type) {
        }

        private final List<Message> messages = new ArrayList<>();

        @Override
        public void addMessage(PsiElement context, @Nonnull LocalizeValue message, @Nonnull ErrorType type) {
            messages.add(new Message(context, message, type));
        }

        @RequiredReadAction
        void apply(AnnotationHolder holder) {
            for (Message message : messages) {
                addMessageWithFixes(message.context(), message.message(), message.type(), holder);
            }
        }
    }

    @RequiredReadAction
    public static void addMessageWithFixes(
        PsiElement context,
        @Nonnull LocalizeValue message,
        @Nonnull Validator.ValidationHost.ErrorType type,
        AnnotationHolder myHolder,
        @Nonnull IntentionAction... fixes
    ) {
        if (message.isNotEmpty()) {
            if (context instanceof XmlTag tag) {
                addMessagesForTag(tag, message, type, myHolder, fixes);
            }
            else {
                HighlightSeverity severity =
                    type == Validator.ValidationHost.ErrorType.ERROR ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
                appendFixes(myHolder.newOfSeverity(severity, message).range(context), fixes);
            }
        }
    }

    @RequiredReadAction
    private static void addMessagesForTag(
        XmlTag tag,
        @Nonnull LocalizeValue message,
        Validator.ValidationHost.ErrorType type,
        AnnotationHolder myHolder,
        IntentionAction... actions
    ) {
        XmlToken childByRole = XmlTagUtil.getStartTagNameElement(tag);

        addMessagesForTreeChild(childByRole, type, message, myHolder, actions);

        childByRole = XmlTagUtil.getEndTagNameElement(tag);
        addMessagesForTreeChild(childByRole, type, message, myHolder, actions);
    }

    @RequiredReadAction
    private static void addMessagesForTreeChild(
        XmlToken childByRole,
        Validator.ValidationHost.ErrorType type,
        @Nonnull LocalizeValue message,
        AnnotationHolder myHolder,
        IntentionAction... actions
    ) {
        if (childByRole != null) {
            HighlightSeverity severity =
                type == Validator.ValidationHost.ErrorType.ERROR ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
            appendFixes(myHolder.newOfSeverity(severity, message).range(childByRole), actions);
        }
    }
}
