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
package com.intellij.javaee;

import javax.annotation.Nonnull;

import consulo.component.extension.ExtensionPointName;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;

import consulo.module.Module;
import consulo.language.psi.PsiFile;
import com.intellij.xml.XmlNSDescriptor;

public interface ImplicitNamespaceDescriptorProvider {
  @NonNls
  ExtensionPointName<ImplicitNamespaceDescriptorProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.implicitNamespaceDescriptorProvider");

  @Nullable
  XmlNSDescriptor getNamespaceDescriptor(@Nullable Module module, @Nonnull final String ns, @Nullable PsiFile file);
}
