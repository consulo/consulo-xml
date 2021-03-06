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
package com.intellij.ide.structureView.xml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.xml.XmlTag;

/**
 * @author peter
 */
public interface XmlStructureViewElementProvider {
  ExtensionPointName<XmlStructureViewElementProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.structureViewElementProvider");

  @Nullable
  StructureViewTreeElement createCustomXmlTagTreeElement(@Nonnull XmlTag tag);
}
