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

import consulo.ui.ex.awt.tree.SimpleTreeStructure;
import consulo.xml.util.xml.DomElement;

public class DomModelTreeStructure extends SimpleTreeStructure {
  private final DomElement myDomElement;
  private AbstractDomElementNode myRootNode;

  public DomModelTreeStructure(DomElement root) {
    myDomElement = root;
  }

  protected AbstractDomElementNode createRoot(DomElement rootElement) {
    return new BaseDomElementNode(myDomElement);
  }

  public AbstractDomElementNode getRootElement() {
    if (myRootNode == null) {
      myRootNode = createRoot(myDomElement);
    }
    return myRootNode;
  }


  public DomElement getRootDomElement() {
    return myDomElement;
  }
}