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

import jakarta.annotation.Nonnull;

import consulo.xml.util.xml.GenericDomValue;
import consulo.language.psi.PsiReference;

/**
 * @author peter
 */
public interface DomElementResolveProblemDescriptor extends DomElementProblemDescriptor{
  @Nonnull
  PsiReference getPsiReference();

  @Nonnull
  GenericDomValue getDomElement();
}
