/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.codeInsight.daemon.impl.quickfix;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.Computable;
import consulo.codeEditor.Editor;
import consulo.container.boot.ContainerPathManager;
import consulo.http.HttpProxyManager;
import consulo.http.HttpRequests;
import consulo.ide.impl.idea.util.net.IOExceptionDialog;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.impl.source.xml.XmlEntityCache;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * @author mike
 */
public class FetchExtResourceAction extends BaseExtResourceAction {
    private static final Logger LOG = Logger.getInstance(FetchExtResourceAction.class);
    private static final String HTML_MIME = "text/html";
    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String FTP_PROTOCOL = "ftp://";
    private static final String EXT_RESOURCES_FOLDER = "extResources";
    private final boolean myForceResultIsValid;

    public FetchExtResourceAction() {
        myForceResultIsValid = false;
    }

    public FetchExtResourceAction(boolean forceResultIsValid) {
        myForceResultIsValid = forceResultIsValid;
    }

    @Nonnull
    @Override
    protected LocalizeValue getQuickFixName() {
        return XmlLocalize.fetchExternalResource();
    }

    @Override
    protected boolean isAcceptableUri(String uri) {
        return uri.startsWith(HTTP_PROTOCOL) || uri.startsWith(FTP_PROTOCOL) || uri.startsWith(HTTPS_PROTOCOL);
    }

    @RequiredReadAction
    public static String findUrl(PsiFile file, int offset, String uri) {
        PsiElement currentElement = file.findElementAt(offset);
        XmlAttribute attribute = PsiTreeUtil.getParentOfType(currentElement, XmlAttribute.class);

        if (attribute != null) {
            XmlTag tag = PsiTreeUtil.getParentOfType(currentElement, XmlTag.class);

            if (tag != null) {
                String prefix = tag.getPrefixByNamespace(XmlUtil.XML_SCHEMA_INSTANCE_URI);
                if (prefix != null) {
                    String attrValue = tag.getAttributeValue(XmlUtil.SCHEMA_LOCATION_ATT, XmlUtil.XML_SCHEMA_INSTANCE_URI);
                    if (attrValue != null) {
                        StringTokenizer tokenizer = new StringTokenizer(attrValue);

                        while (tokenizer.hasMoreElements()) {
                            if (uri.equals(tokenizer.nextToken())) {
                                if (!tokenizer.hasMoreElements()) {
                                    return uri;
                                }
                                String url = tokenizer.nextToken();

                                return url.startsWith(HTTP_PROTOCOL) ? url : uri;
                            }

                            if (!tokenizer.hasMoreElements()) {
                                return uri;
                            }
                            tokenizer.nextToken(); // skip file location
                        }
                    }
                }
            }
        }
        return uri;
    }

    static class FetchingResourceIOException extends IOException {
        private final String url;

        FetchingResourceIOException(Throwable cause, String url) {
            initCause(cause);
            this.url = url;
        }
    }

    @Override
    @RequiredUIAccess
    protected void doInvoke(@Nonnull PsiFile file, int offset, @Nonnull String uri, Editor editor) throws IncorrectOperationException {
        String url = findUrl(file, offset, uri);
        Project project = file.getProject();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, XmlLocalize.fetchingResourceTitle()) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                while (true) {
                    try {
                        HttpProxyManager.getInstance().prepareURL(url);
                        fetchDtd(project, uri, url, indicator);
                        Application.get().invokeLater(() -> DaemonCodeAnalyzer.getInstance(project).restart(file));
                        return;
                    }
                    catch (IOException ex) {
                        LOG.info(ex);
                        @SuppressWarnings("InstanceofCatchParameter") String problemUrl =
                            ex instanceof FetchingResourceIOException ? ((FetchingResourceIOException)ex).url : url;
                        LocalizeValue message = XmlLocalize.errorFetchingTitle();

                        if (!url.equals(problemUrl)) {
                            message = XmlLocalize.errorFetchingDependentResourceTitle();
                        }

                        if (!IOExceptionDialog.showErrorDialog(message.get(), XmlLocalize.errorFetchingResource(problemUrl).get())) {
                            break; // cancel fetching
                        }
                    }
                }
            }
        });
    }

    private void fetchDtd(Project project, String dtdUrl, String url, ProgressIndicator indicator) throws IOException {
        String extResourcesPath = getExternalResourcesPath();
        File extResources = new File(extResourcesPath);
        LOG.assertTrue(extResources.mkdirs() || extResources.exists(), extResources);

        PsiManager psiManager = PsiManager.getInstance(project);
        Application.get().invokeAndWait(
            () -> WriteAction.run(() -> {
                String path = FileUtil.toSystemIndependentName(extResources.getAbsolutePath());
                VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
                LOG.assertTrue(vFile != null, path);
            }),
            indicator.getModalityState()
        );

        List<String> downloadedResources = new LinkedList<>();
        List<String> resourceUrls = new LinkedList<>();
        IOException[] nestedException = new IOException[1];

        try {
            String resPath = fetchOneFile(indicator, url, project, extResourcesPath, null);
            if (resPath == null) {
                return;
            }
            resourceUrls.add(dtdUrl);
            downloadedResources.add(resPath);

            VirtualFile virtualFile = findFileByPath(resPath, dtdUrl, indicator);

            Set<String> linksToProcess = new HashSet<>();
            Set<String> processedLinks = new HashSet<>();
            Map<String, String> baseUrls = new HashMap<>();
            VirtualFile contextFile = virtualFile;
            linksToProcess.addAll(extractEmbeddedFileReferences(virtualFile, null, psiManager, url));

            while (!linksToProcess.isEmpty()) {
                String s = linksToProcess.iterator().next();
                linksToProcess.remove(s);
                processedLinks.add(s);

                boolean absoluteUrl = s.startsWith(HTTP_PROTOCOL);
                String resourceUrl;
                if (absoluteUrl) {
                    resourceUrl = s;
                }
                else {
                    String baseUrl = baseUrls.get(s);
                    if (baseUrl == null) {
                        baseUrl = url;
                    }

                    resourceUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + s;
                }

                String resourcePath;

                String refname = s.substring(s.lastIndexOf('/') + 1);
                if (absoluteUrl) {
                    refname = Integer.toHexString(s.hashCode()) + "_" + refname;
                }
                try {
                    resourcePath = fetchOneFile(indicator, resourceUrl, project, extResourcesPath, refname);
                }
                catch (IOException e) {
                    nestedException[0] = new FetchingResourceIOException(e, resourceUrl);
                    break;
                }

                if (resourcePath == null) {
                    break;
                }

                virtualFile = findFileByPath(resourcePath, absoluteUrl ? s : null, indicator);
                downloadedResources.add(resourcePath);

                if (absoluteUrl) {
                    resourceUrls.add(s);
                }

                Set<String> newLinks = extractEmbeddedFileReferences(virtualFile, contextFile, psiManager, resourceUrl);
                for (String u : newLinks) {
                    baseUrls.put(u, resourceUrl);
                    if (!processedLinks.contains(u)) {
                        linksToProcess.add(u);
                    }
                }
            }
        }
        catch (IOException ex) {
            nestedException[0] = ex;
        }
        if (nestedException[0] != null) {
            cleanup(resourceUrls, downloadedResources);
            throw nestedException[0];
        }
    }

    private static VirtualFile findFileByPath(String resPath, @Nullable String dtdUrl, ProgressIndicator indicator) {
        SimpleReference<VirtualFile> ref = new SimpleReference<>();
        Application.get().invokeAndWait(
            () -> Application.get().runWriteAction(() -> {
                ref.set(LocalFileSystem.getInstance().refreshAndFindFileByPath(resPath.replace(File.separatorChar, '/')));
                if (dtdUrl != null) {
                    ExternalResourceManager.getInstance().addResource(dtdUrl, resPath);
                }
            }),
            indicator.getModalityState()
        );
        return ref.get();
    }

    public static String getExternalResourcesPath() {
        return ContainerPathManager.get().getSystemPath() + File.separator + EXT_RESOURCES_FOLDER;
    }

    private void cleanup(List<String> resourceUrls, List<String> downloadedResources) {
        Application.get().invokeLater(new Runnable() {
            @Override
            public void run() {
                Application.get().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        for (String resourcesUrl : resourceUrls) {
                            ExternalResourceManager.getInstance().removeResource(resourcesUrl);
                        }

                        for (String downloadedResource : downloadedResources) {
                            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(new File(downloadedResource));
                            if (virtualFile != null) {
                                try {
                                    virtualFile.delete(this);
                                }
                                catch (IOException ignore) {
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    @Nullable
    private String fetchOneFile(
        ProgressIndicator indicator,
        String resourceUrl,
        Project project,
        String extResourcesPath,
        @Nullable String refname
    ) throws IOException {
        SwingUtilities.invokeLater(() -> indicator.setTextValue(XmlLocalize.fetchingProgressIndicator(resourceUrl)));

        FetchResult result = fetchData(project, resourceUrl, indicator);
        if (result == null) {
            return null;
        }

        if (!resultIsValid(project, indicator, resourceUrl, result)) {
            return null;
        }

        int slashIndex = resourceUrl.lastIndexOf('/');
        String resPath = extResourcesPath + File.separatorChar;

        if (refname != null) { // resource is known under ref.name so need to save it
            resPath += refname;
            int refNameSlashIndex = resPath.lastIndexOf('/');
            if (refNameSlashIndex != -1) {
                File parent = new File(resPath.substring(0, refNameSlashIndex));
                if (!parent.mkdirs() || !parent.exists()) {
                    LOG.warn("Unable to create: " + parent);
                }
            }
        }
        else {
            resPath += Integer.toHexString(resourceUrl.hashCode()) + "_" + resourceUrl.substring(slashIndex + 1);
        }

        int lastDoPosInResourceUrl = StringUtil.lastIndexOf(resourceUrl, '.', slashIndex, resourceUrl.length());
        String extension = resourceUrl.substring(lastDoPosInResourceUrl + 1);
        if (lastDoPosInResourceUrl == -1 || FileTypeManager.getInstance().getFileTypeByExtension(extension) == UnknownFileType.INSTANCE) {
            // remote url does not contain file with extension
            extension = result.contentType != null && result.contentType.contains(HTML_MIME)
                ? HtmlFileType.INSTANCE.getDefaultExtension()
                : XmlFileType.INSTANCE.getDefaultExtension();
            resPath += "." + extension;
        }

        File res = new File(resPath);

        FileUtil.writeToFile(res, result.bytes);
        return resPath;
    }

    protected boolean resultIsValid(Project project, ProgressIndicator indicator, String resourceUrl, FetchResult result) {
        if (myForceResultIsValid) {
            return true;
        }
        if (!Application.get().isUnitTestMode()
            && result.contentType != null
            && result.contentType.contains(HTML_MIME)
            && new String(result.bytes).contains("<html")) {
            Application.get().invokeLater(
                () -> Messages.showMessageDialog(
                    project,
                    XmlLocalize.invalidUrlNoXmlFileAtLocation(resourceUrl).get(),
                    XmlLocalize.invalidUrlTitle().get(),
                    UIUtil.getErrorIcon()
                ),
                indicator.getModalityState()
            );
            return false;
        }
        return true;
    }

    private static Set<String> extractEmbeddedFileReferences(XmlFile file, XmlFile context, String url) {
        Set<String> result = new LinkedHashSet<>();
        if (context != null) {
            XmlEntityCache.copyEntityCaches(file, context);
        }

        XmlUtil.processXmlElements(
            file,
            element -> {
                if (element instanceof XmlEntityDecl) {
                    String candidateName = null;

                    for (PsiElement e = element.getLastChild(); e != null; e = e.getPrevSibling()) {
                        if (e instanceof XmlAttributeValue && candidateName == null) {
                            candidateName = e.getText().substring(1, e.getTextLength() - 1);
                        }
                        else if (e instanceof XmlToken xmlToken && candidateName != null
                            && (xmlToken.getTokenType() == XmlTokenType.XML_DOCTYPE_PUBLIC
                            || xmlToken.getTokenType() == XmlTokenType.XML_DOCTYPE_SYSTEM)) {
                            if (!result.contains(candidateName)) {
                                result.add(candidateName);
                            }
                            break;
                        }
                    }
                }
                else if (element instanceof XmlTag tag) {
                    String schemaLocation = tag.getAttributeValue(XmlUtil.SCHEMA_LOCATION_ATT);

                    if (schemaLocation != null) {
                        // processing xsd:import && xsd:include
                        PsiReference[] references =
                            tag.getAttribute(XmlUtil.SCHEMA_LOCATION_ATT).getValueElement().getReferences();
                        if (references.length > 0) {
                            String extension = FileUtil.getExtension(new File(url).getName());
                            String namespace = tag.getAttributeValue("namespace");
                            if (namespace != null &&
                                schemaLocation.indexOf('/') == -1 &&
                                !extension.equals(FileUtil.getExtension(schemaLocation))) {
                                result.add(
                                    namespace.substring(0, namespace.lastIndexOf('/') + 1) + schemaLocation
                                );
                            }
                            else {
                                result.add(schemaLocation);
                            }
                        }
                    }
                    else {
                        schemaLocation = tag.getAttributeValue(XmlUtil.SCHEMA_LOCATION_ATT, XmlUtil.XML_SCHEMA_INSTANCE_URI);
                        if (schemaLocation != null) {
                            StringTokenizer tokenizer = new StringTokenizer(schemaLocation);

                            while (tokenizer.hasMoreTokens()) {
                                tokenizer.nextToken();
                                if (!tokenizer.hasMoreTokens()) {
                                    break;
                                }
                                String location = tokenizer.nextToken();
                                result.add(location);
                            }
                        }
                    }
                }

                return true;
            },
            true,
            true
        );
        return result;
    }

    public static Set<String> extractEmbeddedFileReferences(
        VirtualFile vFile,
        @Nullable VirtualFile contextVFile,
        PsiManager psiManager,
        String url
    ) {
        return Application.get().runReadAction((Computable<Set<String>>)() -> {
            PsiFile file = psiManager.findFile(vFile);

            if (file instanceof XmlFile) {
                PsiFile contextFile = contextVFile != null ? psiManager.findFile(contextVFile) : null;
                return extractEmbeddedFileReferences((XmlFile)file, contextFile instanceof XmlFile ? (XmlFile)contextFile : null, url);
            }

            return Collections.emptySet();
        });
    }

    protected static class FetchResult {
        byte[] bytes;
        String contentType;
    }

    @Nullable
    private static FetchResult fetchData(Project project, String dtdUrl, ProgressIndicator indicator) throws IOException {
        try {
            return HttpRequests.request(dtdUrl)
                .accept("text/xml,application/xml,text/html,*/*")
                .connect(request -> {
                    FetchResult result = new FetchResult();
                    result.bytes = request.readBytes(indicator);
                    result.contentType = request.getConnection().getContentType();
                    return result;
                });
        }
        catch (MalformedURLException e) {
            if (!Application.get().isUnitTestMode()) {
                Application.get().invokeLater(
                    () -> Messages.showMessageDialog(
                        project,
                        XmlLocalize.invalidUrlMessage(dtdUrl).get(),
                        XmlLocalize.invalidUrlTitle().get(),
                        UIUtil.getErrorIcon()
                    ),
                    indicator.getModalityState()
                );
            }
        }

        return null;
    }
}
