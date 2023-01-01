/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.util.xml.highlighting;

import consulo.document.util.TextRange;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixProvider;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.psi.PsiReference;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.impl.ConvertContextFactory;
import consulo.xml.util.xml.impl.DomManagerImpl;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class DomElementAnnotationHolderImpl extends SmartList<DomElementProblemDescriptor> implements DomElementAnnotationHolder {
  private static final Logger LOG = Logger.getInstance(DomElementAnnotationHolderImpl.class);
  private final SmartList<Annotation> myAnnotations = new SmartList<Annotation>();
  private final boolean myOnTheFly;

  public DomElementAnnotationHolderImpl(boolean onTheFly) {
    myOnTheFly = onTheFly;
  }

  @Override
  public boolean isOnTheFly() {
    return myOnTheFly;
  }

  @Nonnull
  public DomElementProblemDescriptor createProblem(@Nonnull DomElement domElement, @Nullable String message, LocalQuickFix... fixes) {
    return createProblem(domElement, HighlightSeverity.ERROR, message, fixes);
  }

  @Nonnull
  public DomElementProblemDescriptor createProblem(@Nonnull DomElement domElement,
                                                   DomCollectionChildDescription childDescription,
                                                   @Nullable String message) {
    return addProblem(new DomCollectionProblemDescriptorImpl(domElement, message, HighlightSeverity.ERROR, childDescription));
  }

  @Nonnull
  public final DomElementProblemDescriptor createProblem(@Nonnull DomElement domElement, HighlightSeverity highlightType, String message) {
    return createProblem(domElement, highlightType, message, LocalQuickFix.EMPTY_ARRAY);
  }

  public DomElementProblemDescriptor createProblem(@Nonnull final DomElement domElement,
                                                   final HighlightSeverity highlightType,
                                                   final String message,
                                                   final LocalQuickFix[] fixes) {
    return createProblem(domElement, highlightType, message, null, fixes);
  }

  public DomElementProblemDescriptor createProblem(@Nonnull final DomElement domElement,
                                                   final HighlightSeverity highlightType,
                                                   final String message,
                                                   final TextRange textRange,
                                                   final LocalQuickFix... fixes) {
    return addProblem(new DomElementProblemDescriptorImpl(domElement, message, highlightType, textRange, null, fixes));
  }

  public DomElementProblemDescriptor createProblem(@Nonnull DomElement domElement,
                                                   ProblemHighlightType highlightType,
                                                   String message,
                                                   @Nullable TextRange textRange,
                                                   LocalQuickFix... fixes) {
    return addProblem(new DomElementProblemDescriptorImpl(domElement, message, HighlightSeverity.ERROR, textRange, highlightType, fixes));
  }

  @Nonnull
  public DomElementResolveProblemDescriptor createResolveProblem(@Nonnull GenericDomValue element, @Nonnull PsiReference reference) {
    return addProblem(new DomElementResolveProblemDescriptorImpl(element, reference, getQuickFixes(element, reference)));
  }

  @Nonnull
  public Annotation createAnnotation(@Nonnull DomElement element, HighlightSeverity severity, @Nullable String message) {
    final XmlElement xmlElement = element.getXmlElement();
    LOG.assertTrue(xmlElement != null, "No XML element for " + element);
    final TextRange range = xmlElement.getTextRange();
    final int startOffset = range.getStartOffset();
    final int endOffset = message == null ? startOffset : range.getEndOffset();
    final Annotation annotation = new Annotation(startOffset, endOffset, severity, message, null);
    myAnnotations.add(annotation);
    return annotation;
  }

  public final SmartList<Annotation> getAnnotations() {
    return myAnnotations;
  }

  public int getSize() {
    return size();
  }

  private LocalQuickFix[] getQuickFixes(final GenericDomValue element, PsiReference reference) {
    if (!myOnTheFly) return LocalQuickFix.EMPTY_ARRAY;

    final List<LocalQuickFix> result = new SmartList<LocalQuickFix>();
    final Converter converter = WrappingConverter.getDeepestConverter(element.getConverter(), element);
    if (converter instanceof ResolvingConverter) {
      final ResolvingConverter resolvingConverter = (ResolvingConverter) converter;
      ContainerUtil
          .addAll(result, resolvingConverter.getQuickFixes(ConvertContextFactory.createConvertContext(DomManagerImpl.getDomInvocationHandler(element))));
    }
    if (reference instanceof LocalQuickFixProvider) {
      final LocalQuickFix[] localQuickFixes = ((LocalQuickFixProvider) reference).getQuickFixes();
      if (localQuickFixes != null) {
        ContainerUtil.addAll(result, localQuickFixes);
      }
    }
    return result.isEmpty() ? LocalQuickFix.EMPTY_ARRAY : result.toArray(new LocalQuickFix[result.size()]);
  }

  public <T extends DomElementProblemDescriptor> T addProblem(final T problemDescriptor) {
    add(problemDescriptor);
    return problemDescriptor;
  }

}
