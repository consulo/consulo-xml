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

package com.intellij.util.xml.highlighting;

import consulo.ide.impl.idea.util.Function;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.ProblemDescriptor;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.language.psi.PsiFile;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.ide.impl.idea.util.Consumer;
import com.intellij.util.xml.*;
import com.intellij.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Dmitry Avdeev
 * @see BasicDomElementsInspection
 */
public abstract class DomElementsInspection<T extends DomElement> extends XmlSuppressableInspectionTool {
  private static final Logger LOG = Logger.getInstance(DomElementsInspection.class);

  private final Set<Class<? extends T>> myDomClasses;

  public DomElementsInspection(Class<? extends T> domClass, @Nonnull Class<? extends T>... additionalClasses) {
    myDomClasses = new HashSet<Class<? extends T>>(Arrays.asList(additionalClasses));
    myDomClasses.add(domClass);
  }

  /**
   * This method is called internally in {@link DomElementAnnotationsManager#checkFileElement(DomFileElement, DomElementsInspection, boolean)}
   * it should add some problems to the annotation holder. The default implementation performs recursive tree traversal, and calls
   * {@link #checkDomElement(DomElement, DomElementAnnotationHolder, DomHighlightingHelper)} for each element.
   * @param domFileElement file element to check
   * @param holder the place to store problems
   */
  public void checkFileElement(DomFileElement<T> domFileElement, final DomElementAnnotationHolder holder) {
    final DomHighlightingHelper helper =
      DomElementAnnotationsManager.getInstance(domFileElement.getManager().getProject()).getHighlightingHelper();
    final Consumer<DomElement> consumer = new Consumer<DomElement>() {
      public void consume(final DomElement element) {
        checkChildren(element, this);
        checkDomElement(element, holder, helper);
      }
    };
    consumer.consume(domFileElement.getRootElement());
  }

  @SuppressWarnings({"MethodMayBeStatic"})
  protected void checkChildren(final DomElement element, Consumer<DomElement> visitor) {
    final XmlElement xmlElement = element.getXmlElement();
    if (xmlElement instanceof XmlTag) {
      for (final DomElement child : DomUtil.getDefinedChildren(element, true, true)) {
        final XmlElement element1 = child.getXmlElement();
        if (element1 == null) {
          LOG.error("child=" + child + " of class " + child.getClass() + "; parent=" + element);
        }
        if (element1.isPhysical()) {
          visitor.consume(child);
        }
      }

      for (final AbstractDomChildrenDescription description : element.getGenericInfo().getChildrenDescriptions()) {
        if (description.getAnnotation(Required.class) != null) {
          for (final DomElement child : description.getValues(element)) {
            if (!DomUtil.hasXml(child)) {
              visitor.consume(child);
            }
          }
        }
      }
    }
  }

  /**
   * @return the classes passed earlier to the constructor
   */
  public final Set<Class<? extends T>> getDomClasses() {
    return myDomClasses;
  }

  /**
   * Not intended to be overridden or called by implementors.
   * Override {@link #checkFileElement(DomFileElement, DomElementAnnotationHolder)} (which is preferred) or
   * {@link #checkDomElement(DomElement, DomElementAnnotationHolder, DomHighlightingHelper)} instead.
   */
  @Nullable
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    if (file instanceof XmlFile && (file.isPhysical() || ApplicationManager.getApplication().isUnitTestMode())) {
      for (Class<? extends T> domClass: myDomClasses) {
        final DomFileElement<? extends T> fileElement = DomManager.getDomManager(file.getProject()).getFileElement((XmlFile)file, domClass);
        if (fileElement != null) {
          //noinspection unchecked
          return checkDomFile((DomFileElement<T>)fileElement, manager, isOnTheFly);
        }
      }
    }
    return null;
  }

  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  public boolean isEnabledByDefault() {
    return true;
  }

  /**
   * not intended to be overridden or called by implementors
   */
  @Nullable
  protected ProblemDescriptor[] checkDomFile(@Nonnull final DomFileElement<T> domFileElement,
                                             @Nonnull final InspectionManager manager,
                                             @SuppressWarnings("UnusedParameters") final boolean isOnTheFly) {
    final DomElementAnnotationsManager annotationsManager = DomElementAnnotationsManager.getInstance(manager.getProject());

    final List<DomElementProblemDescriptor> list = annotationsManager.checkFileElement(domFileElement, this, isOnTheFly);
    if (list.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY;

    List<ProblemDescriptor> problems =
      ContainerUtil.concat(list, new Function<DomElementProblemDescriptor, Collection<? extends ProblemDescriptor>>() {
        public Collection<ProblemDescriptor> fun(final DomElementProblemDescriptor s) {
          return annotationsManager.createProblemDescriptors(manager, s);
        }
      });
    return problems.toArray(new ProblemDescriptor[problems.size()]);
  }

  /**
   * Check particular DOM element for problems. The inspection implementor should focus on this method.
   * The default implementation throws {@link UnsupportedOperationException}.
   * See {@link BasicDomElementsInspection}
   * @param element element to check
   * @param holder a place to add problems to
   * @param helper helper object
   */
  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    throw new UnsupportedOperationException("checkDomElement() is not implemented in " + getClass().getName());
  }
}
