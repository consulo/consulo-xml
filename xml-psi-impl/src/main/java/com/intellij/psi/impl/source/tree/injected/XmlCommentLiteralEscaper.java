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
package com.intellij.psi.impl.source.tree.injected;

import javax.annotation.Nonnull;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.impl.source.xml.XmlCommentImpl;

/**
 * @author spleaner
 */
public class XmlCommentLiteralEscaper extends LiteralTextEscaper<XmlCommentImpl> {
  public XmlCommentLiteralEscaper(@Nonnull XmlCommentImpl host) {
    super(host);
  }

  public boolean decode(@Nonnull final TextRange rangeInsideHost, @Nonnull final StringBuilder outChars) {
    ProperTextRange.assertProperRange(rangeInsideHost);
    outChars.append(myHost.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
    return true;
  }

  public int getOffsetInHost(final int offsetInDecoded, @Nonnull final TextRange rangeInsideHost) {
    int offset = offsetInDecoded + rangeInsideHost.getStartOffset();
    if (offset < rangeInsideHost.getStartOffset()) offset = rangeInsideHost.getStartOffset();
    if (offset > rangeInsideHost.getEndOffset()) offset = rangeInsideHost.getEndOffset();
    return offset;
  }

  public boolean isOneLine() {
    final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(myHost.getLanguage());
    if (commenter instanceof CodeDocumentationAwareCommenter) {
      return myHost.getTokenType() == ((CodeDocumentationAwareCommenter) commenter).getLineCommentTokenType();
    }
    return false;
  }
}
