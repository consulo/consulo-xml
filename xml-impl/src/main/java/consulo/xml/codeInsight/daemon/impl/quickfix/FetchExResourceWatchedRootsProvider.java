package consulo.xml.codeInsight.daemon.impl.quickfix;

import consulo.logging.Logger;
import consulo.project.content.WatchedRootsProvider;

import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-07-14
 */
public class FetchExResourceWatchedRootsProvider implements WatchedRootsProvider {
    private static final Logger LOG = Logger.getInstance(FetchExResourceWatchedRootsProvider.class);

    @Nonnull
    @Override
    public Set<String> getRootsToWatch() {
        File path = new File(FetchExtResourceAction.getExternalResourcesPath());
        if (!path.exists() && !path.mkdirs()) {
            LOG.warn("Unable to create: " + path);
        }
        return Collections.singleton(path.getAbsolutePath());
    }
}
