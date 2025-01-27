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
package consulo.xml.patterns;

import jakarta.annotation.Nonnull;

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.InitialPatternCondition;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PsiFilePattern;
import consulo.language.util.ProcessingContext;

/**
 * @author spleaner
 */
public class XmlFilePattern<Self extends XmlFilePattern<Self>> extends PsiFilePattern<XmlFile, Self> {

  public XmlFilePattern() {
    super(XmlFile.class);
  }

  protected XmlFilePattern(@Nonnull final InitialPatternCondition<XmlFile> condition) {
    super(condition);
  }

  public Self withRootTag(final ElementPattern<XmlTag> rootTag) {
    return with(new PatternCondition<XmlFile>("withRootTag") {
      public boolean accepts(@Nonnull final XmlFile xmlFile, final ProcessingContext context) {
        XmlDocument document = xmlFile.getDocument();
        return document != null && rootTag.getCondition().accepts(document.getRootTag(), context);
      }
    });
  }

  public static class Capture extends XmlFilePattern<Capture> {
  }
}
