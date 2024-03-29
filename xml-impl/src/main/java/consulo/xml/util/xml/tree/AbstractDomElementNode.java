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

import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomUtil;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import java.lang.reflect.Type;
import java.util.*;

abstract public class AbstractDomElementNode extends SimpleNode {

  public static final Key<Map<Class, Boolean>> TREE_NODES_HIDERS_KEY = Key.create("TREE_NODES_HIDERS_KEY");

  private final static Comparator<Class> INHERITORS_COMPARATOR = new Comparator<Class>() {
    public int compare(final Class o1, final Class o2) {
      return o1.isAssignableFrom(o2) ? 1 : -1;
    }
  };

  private boolean isExpanded;
  protected final Project myProject;

  protected AbstractDomElementNode(DomElement element) {
    this(element, null);
  }

  public String toString() {
    return getNodeName();
  }

  protected AbstractDomElementNode(DomElement element, SimpleNode parent) {
    super(element.getManager().getProject(), parent);
    myProject = element.getManager().getProject();
  }

  abstract public DomElement getDomElement();

  abstract public String getNodeName();

  abstract public String getTagName();


  public Image getNodeIcon() {
    return getDomElement().getPresentation().getIcon();
  }

  protected String getPropertyName() {
    return getDomElement().getPresentation().getTypeName();
  }

  protected boolean shouldBeShown(final Type type) {
    final Map<Class, Boolean> hiders = DomUtil.getFile(getDomElement()).getUserData(TREE_NODES_HIDERS_KEY);
    if (type == null || hiders == null || hiders.size() == 0) return true;

    final Class aClass = ReflectionUtil.getRawType(type);

    List<Class> allParents = new ArrayList<Class>();
    for (Map.Entry<Class, Boolean> entry : hiders.entrySet()) {
      if (entry.getKey().isAssignableFrom(aClass)) {
        allParents.add(entry.getKey());
      }
    }
    if (allParents.size() == 0) return false;

    Collections.sort(allParents, INHERITORS_COMPARATOR);

    return hiders.get(allParents.get(0)).booleanValue();

  }

  protected SimpleTextAttributes getSimpleAttributes(@SimpleTextAttributes.StyleAttributeConstant final int style) {
    return new SimpleTextAttributes(style, SimpleTextAttributes.REGULAR_ATTRIBUTES.getFgColor());
  }

  protected SimpleTextAttributes getWavedAttributes(@SimpleTextAttributes.StyleAttributeConstant int style) {
    return new SimpleTextAttributes(style | SimpleTextAttributes.STYLE_WAVED, SimpleTextAttributes.REGULAR_ATTRIBUTES.getFgColor(), SimpleTextAttributes.ERROR_ATTRIBUTES.getFgColor());
  }
  public boolean isExpanded() {
    return isExpanded;
  }

  public void setExpanded(final boolean expanded) {
    isExpanded = expanded;
  }
}
