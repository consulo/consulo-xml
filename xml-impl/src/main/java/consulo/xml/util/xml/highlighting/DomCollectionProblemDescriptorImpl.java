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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.localize.LocalizeValue;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class DomCollectionProblemDescriptorImpl extends DomElementProblemDescriptorImpl implements DomCollectionProblemDescriptor {
    private final DomCollectionChildDescription myChildDescription;

    @RequiredReadAction
    public DomCollectionProblemDescriptorImpl(
        DomElement domElement,
        @Nonnull LocalizeValue message,
        HighlightSeverity type,
        DomCollectionChildDescription childDescription
    ) {
        super(domElement, message, type);
        myChildDescription = childDescription;
    }

    @RequiredReadAction
    public DomCollectionProblemDescriptorImpl(
        DomElement domElement,
        @Nonnull LocalizeValue message,
        HighlightSeverity type,
        DomCollectionChildDescription childDescription,
        LocalQuickFix... fixes
    ) {
        super(domElement, message, type, fixes);
        myChildDescription = childDescription;
    }

    @Override
    public String toString() {
        return super.toString() + "; " + myChildDescription;
    }

    @Override
    public DomCollectionChildDescription getChildDescription() {
        return myChildDescription;
    }
}
