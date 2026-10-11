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
package consulo.xml.util.xml.structure;

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.xml.dom.*;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Gregory.Shrago
 */
public class DomStructureTreeElement implements StructureViewTreeElement, ItemPresentation {
    private final DomElement myElement;
    private final Function<DomElement, DomService.StructureViewMode> myDescriptor;
    private final DomElementNavigationProvider myNavigationProvider;

    public DomStructureTreeElement(
        DomElement element,
        Function<DomElement, DomService.StructureViewMode> descriptor,
        @Nullable DomElementNavigationProvider navigationProvider
    ) {
        myElement = element;
        myDescriptor = descriptor;
        myNavigationProvider = navigationProvider;
    }

    public DomElement getElement() {
        return myElement;
    }

    @Override
    public @Nullable Object getValue() {
        return myElement.isValid() ? myElement.getXmlElement() : null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    @Override
    public TreeElement[] getChildren() {
        if (!myElement.isValid()) {
            return EMPTY_ARRAY;
        }
        List<TreeElement> result = new ArrayList<>();
        DomElementVisitor elementVisitor = new DomElementVisitor() {
            @Override
            public void visitDomElement(DomElement element) {
                if (element instanceof GenericDomValue) {
                    return;
                }
                DomService.StructureViewMode viewMode = myDescriptor.apply(element);
                switch (viewMode) {
                    case SHOW:
                        result.add(createChildElement(element));
                        break;
                    case SHOW_CHILDREN:
                        DomUtil.acceptAvailableChildren(element, this);
                        break;
                    case SKIP:
                        break;
                }
            }
        };
        DomUtil.acceptAvailableChildren(myElement, elementVisitor);
        return result.toArray(new TreeElement[result.size()]);
    }

    protected StructureViewTreeElement createChildElement(DomElement element) {
        return new DomStructureTreeElement(element, myDescriptor, myNavigationProvider);
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        if (myNavigationProvider != null) {
            myNavigationProvider.navigate(myElement, true);
        }
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return myNavigationProvider != null && myNavigationProvider.canNavigate(myElement);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return myNavigationProvider != null && myNavigationProvider.canNavigate(myElement);
    }

    @Override
    public LocalizeValue getPresentableText() {
        if (!myElement.isValid()) {
            return LocalizeValue.localizeTODO("<unknown>");
        }
        ElementPresentation presentation = myElement.getPresentation();
        String name = presentation.getElementName();
        return name != null ? LocalizeValue.of(name) : LocalizeValue.of(presentation.getTypeName());
    }

    @Override
    public @Nullable String getLocationString() {
        return null;
    }

    @Override
    public @Nullable Image getIcon() {
        if (!myElement.isValid()) {
            return null;
        }
        return myElement.getPresentation().getIcon();
    }
}
