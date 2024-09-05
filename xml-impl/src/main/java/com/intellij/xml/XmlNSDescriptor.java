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
package com.intellij.xml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.psi.meta.PsiMetaData;

/**
 * @author Mike
 */
public interface XmlNSDescriptor extends PsiMetaData {
    @Nullable
    XmlElementDescriptor getElementDescriptor(@Nonnull XmlTag tag);

    @Nonnull
    XmlElementDescriptor[] getRootElementsDescriptors(@Nullable final XmlDocument document);

    @Nullable
    XmlFile getDescriptorFile();
}
