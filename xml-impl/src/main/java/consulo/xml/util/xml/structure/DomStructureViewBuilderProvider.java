/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.xml.ide.structureView.xml.XmlStructureViewBuilderProvider;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.DomService;

import jakarta.annotation.Nonnull;
import java.util.function.Function;

/**
 * This SHOULD NOT be subclassed!
 *
 * @author Dmitry Avdeev
 * @since 2012-06-07
 */
@ExtensionImpl(order = "last")
public final class DomStructureViewBuilderProvider implements XmlStructureViewBuilderProvider {

  @Override
  public StructureViewBuilder createStructureViewBuilder(@Nonnull XmlFile file) {
    if (DomManager.getDomManager(file.getProject()).getDomFileDescription(file) != null) {
      return new DomStructureViewBuilder(file, DESCRIPTOR);
    }
    return null;
  }

  public static final Function<DomElement,DomService.StructureViewMode> DESCRIPTOR = element -> DomService.StructureViewMode.SHOW;
}
