package org.intellij.plugins.relaxNG.validation;

import com.thaiopensource.resolver.AbstractResolver;
import com.thaiopensource.resolver.Identifier;
import com.thaiopensource.resolver.Input;
import com.thaiopensource.resolver.ResolverException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.net.URI;

/**
 * @author VISTALL
 * @since 2025-03-22
 */
public class VirtualFileSystemResolver extends AbstractResolver {
    @Override
    public void resolve(Identifier id, Input input) throws IOException, ResolverException {
        if (!input.isResolved()) {
            String uri = resolveUri(id);
            if (uri != null) {
                input.setUri(uri);
            }
        }
    }

    @Override
    public void open(Input input) throws IOException, ResolverException {
        if (!input.isUriDefinitive()) {
            return;
        }

        VirtualFile file = VirtualFileManager.getInstance().refreshAndFindFileByUrl(input.getUri());
        if (file != null) {
            input.setByteStream(file.getInputStream());
        } else {
            throw new IOException("Can't find file: " + input.getUri());
        }
    }

    @Nullable
    public static String resolveUri(Identifier id) throws ResolverException {
        try {
            final String uriRef = id.getUriReference();

            URI uri = new URI(uriRef);

            if (!uri.isAbsolute()) {
                String base = id.getBase();
                if (base != null) {
                    VirtualFile baseFile = VirtualFileManager.getInstance().findFileByUrl(base);
                    if (baseFile == null) {
                        URI baseUri = new URI(base);

                        baseFile = VirtualFileUtil.findFileByURL(baseUri.toURL());
                        if (baseFile == null && base.startsWith("jar:")) {
                            String newZipUrl = VirtualFileUtil.convertFromUrl(baseUri.toURL());
                            newZipUrl = "zip:" + newZipUrl.substring(4);

                            baseFile = VirtualFileManager.getInstance().findFileByUrl(newZipUrl);
                        }
                    }

                    if (baseFile != null && !baseFile.isDirectory()) {
                        VirtualFile relativeFile = baseFile.getParent().findFileByRelativePath(uriRef);
                        if (relativeFile != null) {
                            return relativeFile.getUrl();
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            throw new ResolverException(e);
        }

        return null;
    }
}
