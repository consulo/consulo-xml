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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.xml.util.xml.DomElement;
import consulo.component.extension.ExtensionPointName;

import jakarta.annotation.Nonnull;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Basic DOM inspection (see {@link BasicDomElementsInspection})
 * calls this annotator on all DOM elements with the given custom user-defined annotation.
 *
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DomCustomAnnotationChecker<T extends Annotation> {
  public static final ExtensionPointName<DomCustomAnnotationChecker> EP_NAME = ExtensionPointName.create(DomCustomAnnotationChecker.class);
  
  @Nonnull
  public abstract Class<T> getAnnotationClass();

  public abstract List<DomElementProblemDescriptor> checkForProblems(@Nonnull T t, @Nonnull DomElement element, @Nonnull DomElementAnnotationHolder holder, @Nonnull DomHighlightingHelper helper);
}
