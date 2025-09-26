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
package consulo.xml.util.xml.tree;

import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.image.Image;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.ElementPresentationManager;
import consulo.xml.util.xml.ElementPresentationTemplate;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementsProblemsHolder;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class DomElementsGroupNode extends AbstractDomElementNode {
  private final DomElement myParentElement;
  private final DomElement myRootDomElement;
  private final String myChildrenTagName;
  private final DomCollectionChildDescription myChildDescription;

  public DomElementsGroupNode(final DomElement modelElement, DomCollectionChildDescription description, SimpleNode parent,
                              final DomElement rootDomElement) {
    super(modelElement, parent);
    myParentElement = modelElement;
    myChildDescription = description;
    myChildrenTagName = description.getXmlElementName();
    myRootDomElement = rootDomElement;
  }

  public SimpleNode[] getChildren() {
    if (!myParentElement.isValid()) return NO_CHILDREN;

    final List<SimpleNode> simpleNodes = new ArrayList<SimpleNode>();
    for (DomElement domChild : myChildDescription.getStableValues(myParentElement)) {
      if (shouldBeShown(domChild.getDomElementType())) {
        simpleNodes.add(new BaseDomElementNode(domChild, myRootDomElement, this));
      }
    }
    return simpleNodes.toArray(new SimpleNode[simpleNodes.size()]);
  }

  @Nonnull
  public Object[] getEqualityObjects() {
    return new Object[]{myParentElement, myChildrenTagName};
  }

  protected void doUpdate() {
    setIcon(getNodeIcon());

    clearColoredText();

    final boolean showErrors = hasErrors();
    final int childrenCount = getChildren().length;

    if (childrenCount > 0) {
      final SimpleTextAttributes textAttributes =
        showErrors ? getWavedAttributes(SimpleTextAttributes.STYLE_BOLD) :  new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, SimpleTextAttributes.REGULAR_ATTRIBUTES.getFgColor());

      addColoredFragment(getNodeName(), textAttributes);
      addColoredFragment(" (" + childrenCount + ')', showErrors ? IdeLocalize.domElementsTreeChildsContainErrors().get() : null,
                         SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
    else {
      addColoredFragment(getNodeName(), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
    }
  }

  private boolean hasErrors() {
    if (!myParentElement.isValid()) return false;

    for (DomElement domElement : myChildDescription.getStableValues(myParentElement)) {
      final DomElementAnnotationsManager annotationsManager = DomElementAnnotationsManager.getInstance(myProject);
      final DomElementsProblemsHolder holder = annotationsManager.getCachedProblemHolder(domElement);
      final List<DomElementProblemDescriptor> problems = holder.getProblems(domElement, true, HighlightSeverity.ERROR);
      if (problems.size() > 0) return true;
    }

    return false;
  }

  public String getNodeName() {
    if (!myParentElement.isValid()) return "";

    return myChildDescription.getCommonPresentableName(myParentElement);
  }

  public String getTagName() {
    return myChildrenTagName;
  }

  public DomElement getDomElement() {
    return myParentElement;
  }


  public DomCollectionChildDescription getChildDescription() {
    return myChildDescription;
  }


  public Image getNodeIcon() {
    Class clazz = ReflectionUtil.getRawType(myChildDescription.getType());
//        Class arrayClass = Array.newInstance(clazz, 0).getClass();
    ElementPresentationTemplate template = myChildDescription.getPresentationTemplate();
    if (template != null) {
      return template.createPresentation(null).getIcon();
    }
    return ElementPresentationManager.getIconForClass(clazz);
  }
}
