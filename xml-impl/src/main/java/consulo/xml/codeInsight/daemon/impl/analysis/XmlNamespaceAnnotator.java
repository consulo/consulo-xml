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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.util.XmlTagUtil;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * Date: 17.10.13
 */
public class XmlNamespaceAnnotator implements Annotator {
  @Override
  public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
    if (element instanceof XmlTag) {
      XmlTag tag = (XmlTag) element;
      String namespace = tag.getNamespace();

      TextAttributesKey key = XmlNSColorProvider.EP_NAME.computeSafeIfAny(it -> it.getKeyForNamespace(namespace, tag));
      if (key != null) {
        TextRange range = XmlTagUtil.getStartTagRange(tag);
        if (range != null) {
          holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(key);
        }
        TextRange endTagRange = XmlTagUtil.getEndTagRange(tag);
        if (endTagRange != null) {
          holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(endTagRange)
            .textAttributes(key);
        }
      }
    }
  }
}
