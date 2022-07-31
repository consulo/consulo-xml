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
package consulo.xml.codeInsight.editorActions;

import consulo.util.lang.function.Condition;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlElement;

/**
 * @author yole
 */
public class XmlBasicWordSelectionFilter implements Condition<PsiElement> {
  public boolean value(final PsiElement e) {
    return !(e instanceof XmlToken) &&
           !(e instanceof XmlElement);
  }
}