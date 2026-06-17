package consulo.xml.lexer;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.util.pointer.NamedPointer;
import consulo.language.Language;
import consulo.language.LanguageRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 24/05/2023
 */
@Deprecated
@DeprecationInfo("Find another way, this create hard reference to other plugins")
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class ExternalPluginHelper {
    private final NamedPointer<Language> myCSSLanguage;
    private final NamedPointer<Language> myJavaScriptLanguage;

    @Inject
    public ExternalPluginHelper(LanguageRegistry languageRegistry) {
        myCSSLanguage = languageRegistry.createLanguagePointer("CSS");
        myJavaScriptLanguage = languageRegistry.createLanguagePointer("JavaScript");
    }

    @Nullable
    @Deprecated
    public static Language getCssLanguage() {
        return Application.get().getInstance(ExternalPluginHelper.class).myCSSLanguage.get();
    }

    @Nullable
    @Deprecated
    public static Language getJavaScriptLanguage() {
        return Application.get().getInstance(ExternalPluginHelper.class).myJavaScriptLanguage.get();
    }
}
