/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xml.util;

import com.intellij.html.impl.RelaxedHtmlFromSchemaNSDescriptor;
import com.intellij.xml.impl.dtd.XmlNSDescriptorImpl;
import com.intellij.xml.impl.schema.NamedObjectDescriptor;
import com.intellij.xml.impl.schema.SchemaNSDescriptor;
import com.intellij.xml.impl.schema.XmlAttributeDescriptorImpl;
import com.intellij.xml.impl.schema.XmlElementDescriptorImpl;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.filter.AndFilter;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.psi.filter.ContentFilter;
import consulo.language.psi.filter.OrFilter;
import consulo.language.psi.meta.MetaDataContributor;
import consulo.language.psi.meta.MetaDataRegistrar;
import consulo.xml.psi.filters.XmlTagFilter;
import consulo.xml.psi.filters.XmlTextFilter;
import consulo.xml.psi.filters.position.NamespaceFilter;
import consulo.xml.psi.filters.position.TargetNamespaceFilter;
import consulo.xml.psi.xml.*;

/**
 * @author Maxim.Mossienko
 */
@ExtensionImpl
public class XmlMetaDataContributor implements MetaDataContributor {
  @Override
  public void contributeMetaData(final MetaDataRegistrar registrar) {
    {
      registrar.registerMetaData(new AndFilter(new NamespaceFilter(XmlUtil.SCHEMA_URIS), new ClassFilter(XmlDocument.class)), SchemaNSDescriptor::new);

      registrar.registerMetaData(new AndFilter(XmlTagFilter.INSTANCE, new NamespaceFilter(XmlUtil.SCHEMA_URIS), new XmlTextFilter("schema")), SchemaNSDescriptor::new);
    }
    {
      registrar.registerMetaData(new OrFilter(new AndFilter(new ContentFilter(new OrFilter(new ClassFilter(XmlElementDecl.class), new ClassFilter(XmlEntityDecl.class), new ClassFilter
          (XmlConditionalSection.class), new ClassFilter(XmlEntityRef.class))), new ClassFilter(XmlDocument.class)), new ClassFilter(XmlMarkupDecl.class)), XmlNSDescriptorImpl::new);
    }

    {
      registrar.registerMetaData(new AndFilter(XmlTagFilter.INSTANCE, new NamespaceFilter(XmlUtil.SCHEMA_URIS), new XmlTextFilter("element")), XmlElementDescriptorImpl::new);
    }

    {
      registrar.registerMetaData(new AndFilter(XmlTagFilter.INSTANCE, new NamespaceFilter(XmlUtil.SCHEMA_URIS), new XmlTextFilter("attribute")), XmlAttributeDescriptorImpl::new);
    }

    {
      registrar.registerMetaData(new ClassFilter(XmlElementDecl.class), com.intellij.xml.impl.dtd.XmlElementDescriptorImpl::new);
    }

    {
      registrar.registerMetaData(new ClassFilter(XmlAttributeDecl.class), com.intellij.xml.impl.dtd.XmlAttributeDescriptorImpl::new);
    }

    {
      registrar.registerMetaData(new AndFilter(new ClassFilter(XmlDocument.class), new TargetNamespaceFilter(XmlUtil.XHTML_URI), new NamespaceFilter(XmlUtil.SCHEMA_URIS)),
          RelaxedHtmlFromSchemaNSDescriptor::new);
    }

    {
      registrar.registerMetaData(new AndFilter(XmlTagFilter.INSTANCE, new NamespaceFilter(XmlUtil.SCHEMA_URIS), new XmlTextFilter("complexType", "simpleType", "group", "attributeGroup")),
          NamedObjectDescriptor::new);
    }
  }
}
