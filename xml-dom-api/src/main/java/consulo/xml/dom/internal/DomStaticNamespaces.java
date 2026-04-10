package consulo.xml.dom.internal;

import consulo.xml.language.psi.XmlTag;

import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public record DomStaticNamespaces(List<String> namespaces) implements Function<XmlTag, List<String>> {
    @Override
    public List<String> apply(XmlTag xmlTag) {
        return namespaces();
    }
}
