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
package consulo.xml.codeInsight.daemon.impl.analysis.encoding;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import consulo.logging.Logger;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlChildRole;
import jakarta.annotation.Nonnull;

/**
 * @author cdr
 */
public class XmlEncodingReferenceProvider extends PsiReferenceProvider {
    private static final Logger LOG = Logger.getInstance(XmlEncodingReferenceProvider.class);
    private static final String CHARSET_PREFIX = "charset=";

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        LOG.assertTrue(element instanceof XmlAttributeValue);
        XmlAttributeValue value = (XmlAttributeValue)element;

        return new PsiReference[]{new XmlEncodingReference(value, value.getValue(), xmlAttributeValueRange(value), 0)};
    }

    @RequiredReadAction
    protected static TextRange xmlAttributeValueRange(XmlAttributeValue xmlAttributeValue) {
        ASTNode valueNode = XmlChildRole.ATTRIBUTE_VALUE_VALUE_FINDER.findChild(xmlAttributeValue.getNode());
        PsiElement toHighlight = valueNode == null ? xmlAttributeValue : valueNode.getPsi();
        TextRange childRange = toHighlight.getTextRange();
        TextRange range = xmlAttributeValue.getTextRange();
        return childRange.shiftRight(-range.getStartOffset());
    }

    @RequiredReadAction
    public static PsiReference[] extractFromContentAttribute(XmlAttributeValue value) {
        String text = value.getValue();
        int start = text.indexOf(CHARSET_PREFIX);
        if (start != -1) {
            start += CHARSET_PREFIX.length();
            int end = text.indexOf(';', start);
            if (end == -1) {
                end = text.length();
            }
            String charsetName = text.substring(start, end);
            TextRange textRange = new TextRange(start, end).shiftRight(xmlAttributeValueRange(value).getStartOffset());
            return new PsiReference[]{new XmlEncodingReference(value, charsetName, textRange, 0)};
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
