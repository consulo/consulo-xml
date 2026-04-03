package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.xml.lang.xml.XMLLanguage;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2022-08-02
 */
@ExtensionImpl
public class XmlNsPrefixAnnotatorFactory implements AnnotatorFactory {
    @Nullable
    @Override
    public Annotator createAnnotator() {
        return new XmlNsPrefixAnnotator();
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
