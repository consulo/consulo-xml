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

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.xml.util.xml.DomElement;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface DomElementsProblemsHolder {

  List<DomElementProblemDescriptor> getProblems(DomElement domElement);

  @Deprecated
  List<DomElementProblemDescriptor> getProblems(DomElement domElement, boolean includeXmlProblems);

  /**
   *
   * @param domElement domElement
   * @param includeXmlProblems IGNORED
   * @param withChildren include children problems
   * @return problems
   */
  List<DomElementProblemDescriptor> getProblems(DomElement domElement, boolean includeXmlProblems, boolean withChildren);

  @Deprecated
  List<DomElementProblemDescriptor> getProblems(DomElement domElement,
                                                final boolean includeXmlProblems,
                                                final boolean withChildren,
                                                HighlightSeverity minSeverity);

  List<DomElementProblemDescriptor> getProblems(DomElement domElement,
                                                final boolean withChildren,
                                                HighlightSeverity minSeverity);

  List<DomElementProblemDescriptor> getAllProblems();

  List<DomElementProblemDescriptor> getAllProblems(@Nonnull DomElementsInspection inspection);

  boolean isInspectionCompleted(@Nonnull DomElementsInspection inspection);
}
