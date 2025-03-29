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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ContainerUtil;
import consulo.xml.editor.XmlHighlighterColors;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Dmitry Avdeev
 * @since 2013-10-25
 */
public class XmlNsPrefixAnnotator implements Annotator {
    @Override
    @RequiredReadAction
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (element instanceof XmlTag || element instanceof XmlAttribute) {
            List<SchemaPrefixReference> references = ContainerUtil.findAll(element.getReferences(), SchemaPrefixReference.class);
            for (SchemaPrefixReference reference : references) {
                TextRange rangeInElement = reference.getRangeInElement();
                if (!rangeInElement.isEmpty()) {
                    TextRange range = rangeInElement.shiftRight(element.getTextRange().getStartOffset());
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(range)
                        .textAttributes(XmlHighlighterColors.XML_NS_PREFIX)
                        .create();
                }
            }
        }
    }
}
