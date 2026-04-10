package consulo.xml.descriptor.xsd;

import consulo.xml.descriptor.XmlElementDescriptor;
import consulo.xml.descriptor.XmlElementDescriptorAwareAboutChildren;
import consulo.xml.language.psi.XmlElement;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public interface XsdXmlElementDescriptor extends XmlElementDescriptor, XmlElementDescriptorAwareAboutChildren {
    @Nullable
    TypeDescriptor getType();

    @Nullable
    TypeDescriptor getType(XmlElement context);
}
