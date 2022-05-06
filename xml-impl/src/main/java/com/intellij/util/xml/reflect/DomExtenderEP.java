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
package com.intellij.util.xml.reflect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.component.extension.AbstractExtensionPointBean;
import consulo.component.extension.ExtensionPointName;
import com.intellij.util.xml.DomElement;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.logging.Logger;
import consulo.project.Project;

/**
 * @author peter
 */
public class DomExtenderEP extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance("#DomExtenderEP");
  public static final ExtensionPointName<DomExtenderEP> EP_NAME = ExtensionPointName.create("com.intellij.xml.dom.extender");

  @Attribute("domClass")
  public String domClassName;
  @Attribute("extenderClass")
  public String extenderClassName;

  private Class<?> myDomClass;
  private DomExtender myExtender;


  @Nullable
  public DomExtensionsRegistrarImpl extend(@Nonnull final Project project, @Nonnull final DomElement element, @Nullable DomExtensionsRegistrarImpl registrar) {
    if (myExtender == null) {
      try {
        myDomClass = findClass(domClassName);
        myExtender = instantiate(extenderClassName, project.getInjectingContainer());
      }
      catch(Exception e) {
        LOG.error(e);
        return null;
      }
    }
    if (myDomClass.isInstance(element)) {
      if (registrar == null) {
        registrar = new DomExtensionsRegistrarImpl();
      }
      myExtender.registerExtensions(element, registrar);
    }
    return registrar;
  }

}