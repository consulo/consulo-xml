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

package consulo.xml.util.xml.highlighting;

import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 * @see BasicDomElementsInspection
 */
public abstract class DomElementsInspection<T extends DomElement, State> extends XmlSuppressableInspectionTool {
  private static final Logger LOG = Logger.getInstance(DomElementsInspection.class);

  private final Set<Class<? extends T>> myDomClasses;

  @SafeVarargs
  public DomElementsInspection(Class<? extends T> domClass, @Nonnull Class<? extends T>... additionalClasses) {
    Set<Class<? extends T>> domClasses = new HashSet<>(Set.of(additionalClasses));
    domClasses.add(domClass);
    myDomClasses = Set.copyOf(domClasses);
  }

  /**
   * This method is called internally in {@link DomElementAnnotationsManager#checkFileElement(DomFileElement, DomElementsInspection, boolean, State)}
   * it should add some problems to the annotation holder. The default implementation performs recursive tree traversal, and calls
   * {@link #checkDomElement(DomElement, DomElementAnnotationHolder, DomHighlightingHelper, Object)} for each element.
   *
   * @param domFileElement file element to check
   * @param holder         the place to store problems
   */
  public void checkFileElement(DomFileElement<T> domFileElement, final DomElementAnnotationHolder holder, State state) {
    final DomHighlightingHelper helper =
      DomElementAnnotationsManager.getInstance(domFileElement.getManager().getProject()).getHighlightingHelper();
    final Consumer<DomElement> consumer = new Consumer<>() {
      public void accept(final DomElement element) {
        checkChildren(element, this);
        checkDomElement(element, holder, helper, state);
      }
    };
    consumer.accept(domFileElement.getRootElement());
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
          visitor.accept(child);
        }
      }

      for (final AbstractDomChildrenDescription description : element.getGenericInfo().getChildrenDescriptions()) {
        if (description.getAnnotation(Required.class) != null) {
          for (final DomElement child : description.getValues(element)) {
            if (!DomUtil.hasXml(child)) {
              visitor.accept(child);
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

  @Nonnull
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder,
                                        final boolean isOnTheFly,
                                        @Nonnull LocalInspectionToolSession session,
                                        @Nonnull Object state) {
    return new PsiElementVisitor() {
      @Override
      @SuppressWarnings("unchecked")
      public void visitFile(PsiFile file) {
        State inspectionState = (State)state;
        addDescriptors(checkFile(file, holder.getManager(), isOnTheFly, inspectionState));
      }

      private void addDescriptors(final ProblemDescriptor[] descriptors) {
        if (descriptors != null) {
          for (ProblemDescriptor descriptor : descriptors) {
            holder.registerProblem(Objects.requireNonNull(descriptor));
          }
        }
      }
    };
  }

  @Override
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not intended to be overridden or called by implementors.
   * Override {@link #checkFileElement(DomFileElement, DomElementAnnotationHolder, Object)} (which is preferred) or
   * {@link #checkDomElement(DomElement, DomElementAnnotationHolder, DomHighlightingHelper, Object)} instead.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly, State state) {
    if (file instanceof XmlFile && file.isPhysical()) {
      for (Class<? extends T> domClass : myDomClasses) {
        final DomFileElement<? extends T> fileElement = DomManager.getDomManager(file.getProject()).getFileElement((XmlFile)file, domClass);
        if (fileElement != null) {
          return checkDomFile((DomFileElement<T>)fileElement, manager, isOnTheFly, state);
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
                                             final boolean isOnTheFly,
                                             @Nonnull State state) {
    final DomElementAnnotationsManager annotationsManager = DomElementAnnotationsManager.getInstance(manager.getProject());

    final List<DomElementProblemDescriptor> list = annotationsManager.checkFileElement(domFileElement, this, isOnTheFly, state);
    if (list.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY;

    List<ProblemDescriptor> problems =
      ContainerUtil.concat(list, s -> annotationsManager.createProblemDescriptors(manager, s));
    return problems.toArray(new ProblemDescriptor[problems.size()]);
  }

  /**
   * Check particular DOM element for problems. The inspection implementor should focus on this method.
   * The default implementation throws {@link UnsupportedOperationException}.
   * See {@link BasicDomElementsInspection}
   *
   * @param element element to check
   * @param holder  a place to add problems to
   * @param helper  helper object
   */
  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper, State state) {
    throw new UnsupportedOperationException("checkDomElement() is not implemented in " + getClass().getName());
  }
}
