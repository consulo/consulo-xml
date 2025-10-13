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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomBundle;
import consulo.xml.util.xml.DomElement;
import consulo.language.editor.inspection.LocalQuickFix;

/**
 * @author Dmitry Avdeev
 */
public class AddDomElementQuickFix<T extends DomElement> implements LocalQuickFix {
    protected final T myElement;
    @Nonnull
    protected final LocalizeValue myName;

    @SuppressWarnings("unchecked")
    public AddDomElementQuickFix(@Nonnull T element) {
        myElement = (T) element.createStableCopy();
        myName = computeName();
    }

    @Nonnull
    public LocalizeValue getName() {
        return myName;
    }

    private LocalizeValue computeName() {
        String name = myElement.getXmlElementName();
        return isTag()
            ? LocalizeValue.localizeTODO(DomBundle.message("add.element.fix.name", name))
            : LocalizeValue.localizeTODO(DomBundle.message("add.attribute.fix.name", name));
    }

    private boolean isTag() {
        return myElement.getXmlElement() instanceof XmlTag;
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        myElement.ensureXmlElementExists();
    }
}