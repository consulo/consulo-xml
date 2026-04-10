package consulo.xml.impl.internal.dom;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.dom.DomSupportProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public record DomSupportProviders(Set<FileType> fileTypes) {
    private static final ExtensionPointCacheKey<DomSupportProvider, DomSupportProviders> KEY = ExtensionPointCacheKey.create("DomSupportProviders", e -> {
        Set<FileType> res = new HashSet<>();
        e.walk(provider -> res.add(provider.getFileType()));
        return new DomSupportProviders(Set.copyOf(res));
    });

    public static boolean isSupported(Application application, FileType fileType) {
        return application.getExtensionPoint(DomSupportProvider.class).getOrBuildCache(KEY).fileTypes().contains(fileType);
    }
}
