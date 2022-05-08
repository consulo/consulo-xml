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
package com.intellij.xml.util;

import consulo.xml.codeInsight.daemon.XmlErrorMessages;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.component.extension.Extensions;
import consulo.language.impl.file.MultiplePsiFilesPerDocumentFileViewProvider;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.file.FileViewProvider;

/**
 * @author Dmitry Avdeev
 */
public class XmlInvalidIdInspection extends XmlDuplicatedIdInspection {

  protected void checkValue(XmlAttributeValue value, XmlFile file, XmlRefCountHolder refHolder, XmlTag tag, ProblemsHolder holder) {

    String idRef = XmlHighlightVisitor.getUnquotedValue(value, tag);

    if (tag instanceof HtmlTag) {
      idRef = idRef.toLowerCase();
    }

    if (XmlUtil.isSimpleValue(idRef, value) && refHolder.isIdReferenceValue(value)) {
      boolean hasIdDeclaration = refHolder.hasIdDeclaration(idRef);
      if (!hasIdDeclaration && tag instanceof HtmlTag) {
        hasIdDeclaration = refHolder.hasIdDeclaration(value.getValue());
      }

      if (!hasIdDeclaration) {
        for(XmlIdContributor contributor: Extensions.getExtensions(XmlIdContributor.EP_NAME)) {
          if (contributor.suppressExistingIdValidation(file)) {
            return;
          }
        }

        final FileViewProvider viewProvider = tag.getContainingFile().getViewProvider();
        if (viewProvider instanceof MultiplePsiFilesPerDocumentFileViewProvider) {
          holder.registerProblem(value, XmlErrorMessages.message("invalid.id.reference"), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                                 new XmlDeclareIdInCommentAction(idRef));

        }
        else {
          holder.registerProblem(value, XmlErrorMessages.message("invalid.id.reference"), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
        }
      }
    }
  }
}
