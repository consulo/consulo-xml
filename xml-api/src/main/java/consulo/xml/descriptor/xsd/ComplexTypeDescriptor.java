package consulo.xml.descriptor.xsd;

import consulo.xml.language.psi.XmlTag;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public interface ComplexTypeDescriptor extends TypeDescriptor {
    XmlTag getDeclaration();
}
