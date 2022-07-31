package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.AndFilter;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.meta.MetaDataContributor;
import consulo.language.psi.meta.MetaDataRegistrar;
import consulo.xml.psi.filters.position.NamespaceFilter;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncDocument;
import org.intellij.plugins.relaxNG.model.descriptors.RngNsDescriptor;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;
import org.intellij.plugins.relaxNG.xml.dom.impl.RngDefineMetaData;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class RelaxNGMetaDataContributor implements MetaDataContributor
{
	@Override
	public void contributeMetaData(MetaDataRegistrar registrar)
	{
		registrar.registerMetaData(new AndFilter(new NamespaceFilter(ApplicationLoader.RNG_NAMESPACE), new ClassFilter(XmlDocument.class)), RngNsDescriptor::new);

		registrar.registerMetaData(new ClassFilter(RncDocument.class), RngNsDescriptor::new);

		registrar.registerMetaData(new ElementFilter()
		{
			@Override
			public boolean isAcceptable(Object element, PsiElement context)
			{
				if(element instanceof XmlTag)
				{
					final XmlTag tag = (XmlTag) element;
					final DomElement domElement = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
					return domElement instanceof RngDefine;
				}
				return false;
			}

			@Override
			public boolean isClassAcceptable(Class hintClass)
			{
				return XmlTag.class.isAssignableFrom(hintClass);
			}
		}, RngDefineMetaData::new);
	}
}
