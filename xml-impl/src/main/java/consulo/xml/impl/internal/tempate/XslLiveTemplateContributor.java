package consulo.xml.impl.internal.tempate;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInsight.template.HtmlContextType;
import consulo.xml.codeInsight.template.XmlContextType;
import consulo.xml.codeInsight.template.XslTextContextType;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class XslLiveTemplateContributor implements LiveTemplateContributor {
    @Override
    @Nonnull
    public String groupId() {
        return "xsl";
    }

    @Override
    @Nonnull
    public LocalizeValue groupName() {
        return LocalizeValue.localizeTODO("XSL");
    }

    @Override
    public void contribute(@Nonnull LiveTemplateContributor.Factory factory) {
        try (Builder builder = factory.newBuilder("xslAi", "ai", "<xsl:apply-imports/>$END$", LocalizeValue.localizeTODO("Apply-Imports."))) {
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslAn", "an", "<xsl:attribute name=\"$NAME$\">$END$</xsl:attribute>", LocalizeValue.localizeTODO("Attribute-Name."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslAts", "ats", "<xsl:apply-templates select=\"$SELECT$\"/>$END$", LocalizeValue.localizeTODO("Apply-Templates-Select."))) {
            builder.withVariable("SELECT", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslAtsm", "atsm", "<xsl:apply-templates select=\"$SELECT$\" mode=\"$MODE$\"/>$END$", LocalizeValue.localizeTODO("Apply-Templates-Select-Mode."))) {
            builder.withVariable("SELECT", "", "", true);
            builder.withVariable("MODE", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }
        try (Builder builder = factory.newBuilder("xslCos", "cos", "<xsl:copy-of select=\"$END$\"/>", LocalizeValue.localizeTODO("Copy-Of-Select."))) {
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }
        try (Builder builder = factory.newBuilder("xslCtn", "ctn", "<xsl:call-template name=\"$NAME$\"/>$END$", LocalizeValue.localizeTODO("Call-Template-Name."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslCtnwp", "ctnwp", "<xsl:call-template name=\"$NAME$\">\n      <xsl:with-param name=\"$PNAME$\" select=\"$SELECT$\"/>\n</xsl:call-template>$END$", LocalizeValue.localizeTODO("Call-Template-Name-With-Param."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("PNAME", "", "", true);
            builder.withVariable("SELECT", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslCwt", "cwt", "<xsl:choose>\n    <xsl:when test=\"$TEST$\">\n        $END$\n    </xsl:when>\n</xsl:choose>", LocalizeValue.localizeTODO("Choose-When-Test."))) {
            builder.withVariable("TEST", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslCwto", "cwto", "<xsl:choose>\n    <xsl:when test=\"$TEST$\">\n        $END$\n    </xsl:when>\n    <xsl:otherwise>\n        \n    </xsl:otherwise>\n</xsl:choose>", LocalizeValue.localizeTODO("Choose-When-Test-Otherwise."))) {
            builder.withVariable("TEST", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslFe", "fe", "<xsl:for-each select=\"$SELECT$\">\n    $END$\n</xsl:for-each>", LocalizeValue.localizeTODO("For-Each."))) {
            builder.withVariable("SELECT", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslIt", "it", "<xsl:if test=\"$TEST$\">\n    $END$\n</xsl:if>", LocalizeValue.localizeTODO("If-Test."))) {
            builder.withVariable("TEST", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslKn", "kn", "<xsl:key name=\"$NAME$\" match=\"$MATCH$\" use=\"$USE$\"/>$END$", LocalizeValue.localizeTODO("Key-Name."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("MATCH", "", "", true);
            builder.withVariable("USE", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslO", "o", "<xsl:otherwise>\n    $END$\n</xsl:otherwise>", LocalizeValue.localizeTODO("Otherwise."))) {
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslPn", "pn", "<xsl:param name=\"$END$\"/>", LocalizeValue.localizeTODO("Param-Name."))) {
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslTm", "tm", "<xsl:template match=\"$MATCH$\">\n    $END$\n</xsl:template>", LocalizeValue.localizeTODO("Template-Match."))) {
            builder.withVariable("MATCH", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslTmm", "tmm", "<xsl:template match=\"$MATCH$\" mode=\"$MODE$\">\n    $END$\n</xsl:template>", LocalizeValue.localizeTODO("Template-Match-Mode."))) {
            builder.withVariable("MATCH", "", "", true);
            builder.withVariable("MODE", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslTn", "tn", "<xsl:template name=\"$NAME$\">\n    $END$\n</xsl:template>", LocalizeValue.localizeTODO("Template-Name."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslTt", "tt", "<xsl:text>$END$</xsl:text>", LocalizeValue.localizeTODO("Text."))) {
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslVn", "vn", "<xsl:variable name=\"$NAME$\">$END$</xsl:variable>", LocalizeValue.localizeTODO("Variable-Name."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslVns", "vns", "<xsl:variable name=\"$NAME$\" select=\"$SELECT$\"/>", LocalizeValue.localizeTODO("Variable-Name-Select."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("SELECT", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslVos", "vos", "<xsl:value-of select=\"$END$\"/>", LocalizeValue.localizeTODO("Value-Of-Select."))) {
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslWp", "wp", "<xsl:with-param name=\"$NAME$\" select=\"$SELECT$\"/>$END$", LocalizeValue.localizeTODO("With-Param."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("SELECT", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslWpn", "wpn", "<xsl:with-param name=\"$NAME$\" select=\"$SELECT$\"/>$END$", LocalizeValue.localizeTODO("With-Param-Name."))) {
            builder.withVariable("NAME", "", "", true);
            builder.withVariable("SELECT", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }

        try (Builder builder = factory.newBuilder("xslWt", "wt", "<xsl:when test=\"$TEST$\">$END$</xsl:when>", LocalizeValue.localizeTODO("When-Test."))) {
            builder.withVariable("TEST", "", "", true);
            builder.withContext(XmlContextType.class, false);
            builder.withContext(XslTextContextType.class, true);
            builder.withContext(HtmlContextType.class, false);
        }
    }
}