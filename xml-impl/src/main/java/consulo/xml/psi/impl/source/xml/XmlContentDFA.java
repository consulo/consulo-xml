/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.xml.psi.impl.source.xml;

import consulo.language.template.TemplateLanguageUtil;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlContentDFA {

  public abstract List<XmlElementDescriptor> getPossibleElements();

  public abstract void transition(XmlTag xmlTag);

  @Nullable
  public static XmlContentDFA getContentDFA(@Nonnull XmlTag parentTag) {

    if (TemplateLanguageUtil.isInsideTemplateFile(parentTag)) return null;

    XmlContentDFA contentDFA = XsContentDFA.createContentDFA(parentTag);
    if (contentDFA != null) return contentDFA;
    return XmlContentDFAImpl.createContentDFA(parentTag);
  }
}
