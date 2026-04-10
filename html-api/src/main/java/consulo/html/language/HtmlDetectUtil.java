package consulo.html.language;

import consulo.annotation.access.RequiredReadAction;
import consulo.html.language.psi.util.HtmlBuilderDriver;
import consulo.localize.LocalizeValue;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.xml.language.psi.parser.XmlBuilder;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public class HtmlDetectUtil {
    private static final String CHARSET = "charset";
    private static final String CHARSET_PREFIX = CHARSET + "=";

    private static class TerminateException extends RuntimeException {
        private static final TerminateException INSTANCE = new TerminateException();
    }

    @RequiredReadAction
    public static Charset detectCharsetFromMetaTag(CharSequence content) {
        // check for <meta http-equiv="charset=CharsetName" > or <meta charset="CharsetName"> and return Charset
        // because we will lightly parse and explicit charset isn't used very often do quick check for applicability
        int charPrefix = StringUtil.indexOf(content, CHARSET);
        do {
            if (charPrefix == -1) {
                return null;
            }
            int charsetPrefixEnd = charPrefix + CHARSET.length();
            while (charsetPrefixEnd < content.length() && Character.isWhitespace(content.charAt(charsetPrefixEnd))) {
                ++charsetPrefixEnd;
            }
            if (charsetPrefixEnd < content.length() && content.charAt(charsetPrefixEnd) == '=') {
                break;
            }
            charPrefix = StringUtil.indexOf(content, CHARSET, charsetPrefixEnd);
        }
        while (true);

        final Ref<String> charsetNameRef = new Ref<>();
        try {
            new HtmlBuilderDriver(content).build(new XmlBuilder() {
                final Set<String> inTag = new HashSet<>();
                boolean metHttpEquiv = false;
                boolean metHttml5Charset = false;

                @Override
                public void doctype(
                    @Nullable final CharSequence publicId,
                    @Nullable final CharSequence systemId,
                    final int startOffset,
                    final int endOffset
                ) {
                }

                @Override
                public ProcessingOrder startTag(
                    final CharSequence localName,
                    final String namespace,
                    final int startoffset,
                    final int endoffset,
                    final int headerEndOffset
                ) {
                    String name = localName.toString().toLowerCase();
                    inTag.add(name);
                    if (!inTag.contains("head") && !"html".equals(name)) {
                        terminate();
                    }
                    return ProcessingOrder.TAGS_AND_ATTRIBUTES;
                }

                private void terminate() {
                    throw TerminateException.INSTANCE;
                }

                @Override
                public void endTag(final CharSequence localName, final String namespace, final int startoffset, final int endoffset) {
                    final String name = localName.toString().toLowerCase();
                    if ("meta".equals(name) && (metHttpEquiv || metHttml5Charset) && contentAttributeValue != null) {
                        String charsetName;
                        if (metHttpEquiv) {
                            int start = contentAttributeValue.indexOf(CHARSET_PREFIX);
                            if (start == -1) {
                                return;
                            }
                            start += CHARSET_PREFIX.length();
                            int end = contentAttributeValue.indexOf(';', start);
                            if (end == -1) {
                                end = contentAttributeValue.length();
                            }
                            charsetName = contentAttributeValue.substring(start, end);
                        }
                        else /*if (metHttml5Charset) */ {
                            charsetName = StringUtil.stripQuotesAroundValue(contentAttributeValue);
                        }
                        charsetNameRef.set(charsetName);
                        terminate();
                    }
                    if ("head".equals(name)) {
                        terminate();
                    }
                    inTag.remove(name);
                    metHttpEquiv = false;
                    metHttml5Charset = false;
                    contentAttributeValue = null;
                }

                private String contentAttributeValue;

                @Override
                public void attribute(final CharSequence localName, final CharSequence v, final int startoffset, final int endoffset) {
                    final String name = localName.toString().toLowerCase();
                    if (inTag.contains("meta")) {
                        String value = v.toString().toLowerCase();
                        if (name.equals("http-equiv")) {
                            metHttpEquiv |= value.equals("content-type");
                        }
                        else if (name.equals(CHARSET)) {
                            metHttml5Charset = true;
                            contentAttributeValue = value;
                        }
                        if (name.equals("content")) {
                            contentAttributeValue = value;
                        }
                    }
                }

                @Override
                public void textElement(
                    final CharSequence display,
                    final CharSequence physical,
                    final int startoffset,
                    final int endoffset
                ) {
                }

                @Override
                public void entityRef(final CharSequence ref, final int startOffset, final int endOffset) {
                }

                @Override
                public void error(LocalizeValue message, int startOffset, int endOffset) {
                }
            });
        }
        catch (TerminateException ignored) {
            //ignore
        }
        catch (Exception ignored) {
            // some weird things can happen, like unbalanaced tree
        }

        String name = charsetNameRef.get();
        return CharsetToolkit.forName(name);
    }
}
