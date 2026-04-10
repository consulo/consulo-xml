package consulo.xml.descriptor.xsd;

import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.xml.descriptor.XmlElementDescriptor;
import consulo.xml.language.XmlSharedUtil;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.util.XmlPsiUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public class XsdSchemeUtil {
    public static final String XSD_SIMPLE_CONTENT_TAG = "simpleContent";
    public static final String ENUMERATION_TAG_NAME = "enumeration";
    public static final String VALUE_ATTR_NAME = "value";

    private final static Set<String> doNotVisitTags = new HashSet<>(Arrays.asList("annotation", "element", "attribute"));

    @Nullable
    public static XmlTag getSchemaSimpleContent(XmlTag tag) {
        XmlElementDescriptor descriptor = tag.getDescriptor();

        if (descriptor instanceof XsdXmlElementDescriptor xsdXmlElementDescriptor) {
            final TypeDescriptor type = xsdXmlElementDescriptor.getType(tag);

            if (type instanceof ComplexTypeDescriptor typeDescriptor) {
                final XmlTag[] simpleContent = new XmlTag[1];

                XmlPsiUtil.processXmlElements(typeDescriptor.getDeclaration(), new PsiElementProcessor() {
                    @Override
                    public boolean execute(final PsiElement element) {
                        if (element instanceof XmlTag) {
                            final XmlTag tag = (XmlTag) element;
                            final String s = ((XmlTag) element).getLocalName();

                            if ((s.equals(XSD_SIMPLE_CONTENT_TAG) || s.equals("restriction")
                                && "string".equals(XmlSharedUtil.findLocalNameByQualifiedName(tag.getAttributeValue("base"))))
                                && XmlSharedUtil.XML_SCHEMA_URI.equals(tag.getNamespace())) {
                                simpleContent[0] = tag;
                                return false;
                            }
                        }

                        return true;
                    }
                }, true);

                return simpleContent[0];
            }
        }
        return null;
    }

    public static boolean collectEnumerationValues(final XmlTag element, final HashSet<String> variants) {
        return processEnumerationValues(
            element,
            xmlTag -> {
                variants.add(xmlTag.getAttributeValue(VALUE_ATTR_NAME));
                return true;
            }
        );
    }

    /**
     * @return true if enumeration is exhaustive
     */
    public static boolean processEnumerationValues(final XmlTag element, final Predicate<XmlTag> tagProcessor) {
        boolean exhaustiveEnum = true;

        for (final XmlTag tag : element.getSubTags()) {
            final String localName = tag.getLocalName();

            if (localName.equals(ENUMERATION_TAG_NAME)) {
                final String attributeValue = tag.getAttributeValue(VALUE_ATTR_NAME);
                if (attributeValue != null && !tagProcessor.test(tag)) {
                    return exhaustiveEnum;
                }
            }
            else if (localName.equals("union")) {
                exhaustiveEnum = false;
                processEnumerationValues(tag, tagProcessor);
            }
            else if (!doNotVisitTags.contains(localName)) {
                // don't go into annotation
                exhaustiveEnum &= processEnumerationValues(tag, tagProcessor);
            }
        }
        return exhaustiveEnum;
    }
}
