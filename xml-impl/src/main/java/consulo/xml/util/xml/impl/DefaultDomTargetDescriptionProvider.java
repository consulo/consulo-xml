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
package consulo.xml.util.xml.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.pom.PomDescriptionProvider;
import consulo.language.pom.PomTarget;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageViewLongNameLocation;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewTypeLocation;
import consulo.util.lang.StringUtil;
import consulo.xml.dom.DomElement;
import consulo.xml.util.xml.DomTargetImpl;
import consulo.xml.dom.ElementPresentation;
import consulo.xml.dom.ElementPresentationTemplate;


/**
 * @author peter
 */
@ExtensionImpl(id = "domDefault", order = "last, before pomDefault")
public class DefaultDomTargetDescriptionProvider extends PomDescriptionProvider {
    @Override
    @RequiredReadAction
    public LocalizeValue getElementDescription(PomTarget element, ElementDescriptionLocation location) {
        if (!(element instanceof DomTargetImpl target)) {
            return LocalizeValue.empty();
        }

        DomElement domElement = target.getDomElement();
        ElementPresentationTemplate template = domElement.getChildDescription().getPresentationTemplate();
        ElementPresentation presentation = template != null ? template.createPresentation(domElement) : domElement.getPresentation();

        if (location == UsageViewTypeLocation.INSTANCE) {
            return LocalizeValue.of(presentation.getTypeName());
        }
        if (location == UsageViewNodeTextLocation.INSTANCE || location == UsageViewLongNameLocation.INSTANCE) {
            return LocalizeValue.localizeTODO(presentation.getTypeName() + " " + StringUtil.notNullize(presentation.getElementName(), "''"));
        }
        if (location instanceof HighlightUsagesDescriptionLocation) {
            return LocalizeValue.of(presentation.getTypeName());
        }
        return LocalizeValue.empty();
    }
}
