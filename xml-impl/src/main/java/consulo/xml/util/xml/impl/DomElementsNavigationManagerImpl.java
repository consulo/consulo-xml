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

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.fileEditor.ex.FileEditorManagerEx;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: Sergey.Vasiliev
 */
@Singleton
@ServiceImpl
public class DomElementsNavigationManagerImpl extends DomElementsNavigationManager {
  private final Map<String, DomElementNavigationProvider> myProviders = new HashMap<String, DomElementNavigationProvider>();
  private final Project myProject;

  private final DomElementNavigationProvider myTextEditorProvider = new MyDomElementNavigateProvider();

  @Inject
  public DomElementsNavigationManagerImpl(final Project project) {
    myProject = project;
    myProviders.put(myTextEditorProvider.getProviderName(), myTextEditorProvider);
  }

  public Set<DomElementNavigationProvider> getDomElementsNavigateProviders(DomElement domElement) {
    Set<DomElementNavigationProvider> result = new HashSet<DomElementNavigationProvider>();

    for (DomElementNavigationProvider navigateProvider : myProviders.values()) {
      if (navigateProvider.canNavigate(domElement)) {
        result.add(navigateProvider);
      }
    }
    return result;
  }

  public DomElementNavigationProvider getDomElementsNavigateProvider(String providerName) {
    return myProviders.get(providerName);
  }

  public void registerDomElementsNavigateProvider(DomElementNavigationProvider provider) {
    myProviders.put(provider.getProviderName(), provider);
  }

  private class MyDomElementNavigateProvider extends DomElementNavigationProvider {

    public String getProviderName() {
      return DEFAULT_PROVIDER_NAME;
    }

    public void navigate(DomElement domElement, boolean requestFocus) {
      if (!domElement.isValid()) {
        return;
      }

      final DomFileElement<DomElement> fileElement = DomUtil.getFileElement(domElement);
      if (fileElement == null) {
        return;
      }

      VirtualFile file = fileElement.getFile().getVirtualFile();
      if (file == null) {
        return;
      }

      XmlElement xmlElement = domElement.getXmlElement();
      if (xmlElement instanceof XmlAttribute) {
        xmlElement = ((XmlAttribute) xmlElement).getValueElement();
      }
      final OpenFileDescriptor fileDescriptor = xmlElement != null ?
          OpenFileDescriptorFactory.getInstance(myProject).builder(file).offset(xmlElement.getTextOffset()).build() :
          OpenFileDescriptorFactory.getInstance(myProject).builder(file).build();

      FileEditorManagerEx.getInstanceEx(myProject).openTextEditor(fileDescriptor, requestFocus);
    }

    public boolean canNavigate(DomElement domElement) {
      return domElement != null && domElement.isValid();
    }
  }
}
