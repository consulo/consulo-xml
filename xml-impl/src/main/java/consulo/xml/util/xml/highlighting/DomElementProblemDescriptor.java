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

import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.xml.util.xml.DomElement;
import consulo.language.editor.inspection.ProblemHighlightType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public interface DomElementProblemDescriptor extends CommonProblemDescriptor {

  @Nonnull
  DomElement getDomElement();
  @Nonnull
  HighlightSeverity getHighlightSeverity();
  @Nonnull
  LocalQuickFix[] getFixes();
  @Nonnull
  List<Annotation> getAnnotations();

  void highlightWholeElement();

  @Nullable
  ProblemHighlightType getHighlightType();
}
