package com.intellij.xml.impl.dom;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import consulo.language.pom.PomService;
import consulo.language.pom.PomTarget;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.EvaluatedXmlName;
import consulo.xml.util.xml.impl.AttributeChildDescriptionImpl;
import consulo.xml.util.xml.reflect.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class AbstractDomChildrenDescriptor implements XmlElementDescriptor {
    protected final DomManager myManager;

    protected AbstractDomChildrenDescriptor(DomManager manager) {
        myManager = manager;
    }

    public XmlElementDescriptor[] getElementsDescriptors(final XmlTag context) {
        final DomElement domElement = myManager.getDomElement(context);
        if (domElement == null) {
            return EMPTY_ARRAY;
        }

        List<XmlElementDescriptor> xmlElementDescriptors = new ArrayList<>();

        for (DomCollectionChildDescription childrenDescription : domElement.getGenericInfo().getCollectionChildrenDescriptions()) {
            xmlElementDescriptors.add(new DomElementXmlDescriptor(childrenDescription, myManager));
        }

        for (DomFixedChildDescription childrenDescription : domElement.getGenericInfo().getFixedChildrenDescriptions()) {
            xmlElementDescriptors.add(new DomElementXmlDescriptor(childrenDescription, myManager));
        }

        final List<? extends CustomDomChildrenDescription> customs = domElement.getGenericInfo().getCustomNameChildrenDescription();

        for (final CustomDomChildrenDescription custom : customs) {
            final CustomDomChildrenDescription.TagNameDescriptor tagNameDescriptor = custom.getTagNameDescriptor();
            if (tagNameDescriptor == null) {
                continue;
            }
            final XmlTag xmlTag = domElement.getXmlTag();
            for (final EvaluatedXmlName name : tagNameDescriptor.getCompletionVariants(domElement)) {
                AbstractDomChildrenDescriptor descriptor = new AbstractDomChildrenDescriptor(myManager) {
                    @Override
                    public String getDefaultName() {
                        final String ns = xmlTag != null ? name.getNamespace(xmlTag, (XmlFile)xmlTag.getContainingFile()) : null;
                        if (ns != null) {
                            final String prefix = xmlTag.getPrefixByNamespace(ns);
                            if (prefix != null) {
                                return prefix + ":" + name.getXmlName().getLocalName();
                            }
                        }
                        return name.getXmlName().getLocalName();
                    }

                    @Override
                    @Nullable
                    public PsiElement getDeclaration() {
                        final PomTarget target = tagNameDescriptor.findDeclaration(domElement, name);
                        return target == null ? null : PomService.convertToPsi(context.getProject(), target);
                    }
                };
                xmlElementDescriptors.add(descriptor);
            }

            xmlElementDescriptors.add(new AnyXmlElementDescriptor(this, getNSDescriptor()));
        }

        return xmlElementDescriptors.toArray(new XmlElementDescriptor[xmlElementDescriptors.size()]);
    }

    @Override
    public XmlElementsGroup getTopGroup() {
        return null;
    }

    @Nullable
    public XmlElementDescriptor getElementDescriptor(@Nonnull final XmlTag childTag, @Nullable XmlTag contextTag) {
        DomElement domElement = myManager.getDomElement(childTag);
        if (domElement == null) {
            domElement = myManager.getDomElement(contextTag);
            if (domElement != null) {
                AbstractDomChildrenDescription description = myManager.findChildrenDescription(childTag, domElement);
                if (description instanceof DomChildrenDescription domChildrenDescription) {
                    return new DomElementXmlDescriptor(domChildrenDescription, myManager);
                }
            }
            return null;
        }

        final DomElement parent = domElement.getParent();
        if (parent == null) {
            return new DomElementXmlDescriptor(domElement);
        }

        final AbstractDomChildrenDescription description = domElement.getChildDescription();
        if (description instanceof CustomDomChildrenDescription customDomChildrenDescription) {
            final DomElement finalDomElement = domElement;
            return new AbstractDomChildrenDescriptor(myManager) {
                @Override
                public String getDefaultName() {
                    return finalDomElement.getXmlElementName();
                }

                @Override
                @Nullable
                public PsiElement getDeclaration() {
                    final PomTarget target = customDomChildrenDescription.getTagNameDescriptor().findDeclaration(finalDomElement);
                    if (target == description) {
                        return childTag;
                    }
                    return target == null ? null : PomService.convertToPsi(childTag.getProject(), target);
                }
            };
        }
        if (!(description instanceof DomChildrenDescription)) {
            return null;
        }

        return new DomElementXmlDescriptor((DomChildrenDescription)description, myManager);
    }

    public XmlAttributeDescriptor[] getAttributesDescriptors(final @Nullable XmlTag context) {
        if (context == null) {
            return XmlAttributeDescriptor.EMPTY;
        }

        DomElement domElement = myManager.getDomElement(context);
        if (domElement == null) {
            return XmlAttributeDescriptor.EMPTY;
        }

        final List<? extends DomAttributeChildDescription> descriptions = domElement.getGenericInfo().getAttributeChildrenDescriptions();
        List<XmlAttributeDescriptor> descriptors = new ArrayList<>();

        for (DomAttributeChildDescription description : descriptions) {
            descriptors.add(new DomAttributeXmlDescriptor(description, myManager.getProject()));
        }
        List<? extends CustomDomChildrenDescription> customs = domElement.getGenericInfo().getCustomNameChildrenDescription();
        for (CustomDomChildrenDescription custom : customs) {
            CustomDomChildrenDescription.AttributeDescriptor descriptor = custom.getCustomAttributeDescriptor();
            if (descriptor != null) {
                for (EvaluatedXmlName variant : descriptor.getCompletionVariants(domElement)) {
                    AttributeChildDescriptionImpl childDescription = new AttributeChildDescriptionImpl(variant.getXmlName(), String.class);
                    descriptors.add(new DomAttributeXmlDescriptor(childDescription, myManager.getProject()));
                }
            }
        }
        return descriptors.toArray(new XmlAttributeDescriptor[descriptors.size()]);
    }

    @Nullable
    public XmlAttributeDescriptor getAttributeDescriptor(final String attributeName, final @Nullable XmlTag context) {
        DomElement domElement = myManager.getDomElement(context);
        if (domElement == null) {
            return null;
        }

        for (DomAttributeChildDescription description : domElement.getGenericInfo().getAttributeChildrenDescriptions()) {
            if (attributeName.equals(DomAttributeXmlDescriptor.getQualifiedAttributeName(context, description.getXmlName()))) {
                return new DomAttributeXmlDescriptor(description, myManager.getProject());
            }
        }
        return null;
    }

    @Nullable
    public XmlAttributeDescriptor getAttributeDescriptor(final XmlAttribute attribute) {
        return getAttributeDescriptor(attribute.getName(), attribute.getParent());
    }

    public XmlNSDescriptor getNSDescriptor() {
        return new XmlNSDescriptor() {
            @Nullable
            public XmlElementDescriptor getElementDescriptor(@Nonnull final XmlTag tag) {
                throw new UnsupportedOperationException("Method getElementDescriptor not implemented in " + getClass());
            }

            @Nonnull
            public XmlElementDescriptor[] getRootElementsDescriptors(@Nullable final XmlDocument document) {
                throw new UnsupportedOperationException("Method getRootElementsDescriptors not implemented in " + getClass());
            }

            @Nullable
            public XmlFile getDescriptorFile() {
                return null;
            }

            public boolean isHierarhyEnabled() {
                throw new UnsupportedOperationException("Method isHierarhyEnabled not implemented in " + getClass());
            }

            @Nullable
            public PsiElement getDeclaration() {
                throw new UnsupportedOperationException("Method getDeclaration not implemented in " + getClass());
            }

            @NonNls
            public String getName(final PsiElement context) {
                throw new UnsupportedOperationException("Method getName not implemented in " + getClass());
            }

            @NonNls
            public String getName() {
                throw new UnsupportedOperationException("Method getName not implemented in " + getClass());
            }

            public void init(final PsiElement element) {
                throw new UnsupportedOperationException("Method init not implemented in " + getClass());
            }

            public Object[] getDependences() {
                throw new UnsupportedOperationException("Method getDependences not implemented in " + getClass());
            }
        };
    }

    public int getContentType() {
        return CONTENT_TYPE_UNKNOWN;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    public void init(final PsiElement element) {
        throw new UnsupportedOperationException("Method init not implemented in " + getClass());
    }

    public Object[] getDependences() {
        throw new UnsupportedOperationException("Method getDependences not implemented in " + getClass());
    }

    @NonNls
    public String getName() {
        return getDefaultName();
    }

    public String getQualifiedName() {
        return getDefaultName();
    }

    @Override
    public String getName(PsiElement context) {
        return getDefaultName();
    }
}
