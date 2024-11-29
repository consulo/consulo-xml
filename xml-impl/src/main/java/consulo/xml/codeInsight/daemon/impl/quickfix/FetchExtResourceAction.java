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

import com.intellij.xml.XmlBundle;
import com.intellij.xml.util.XmlUtil;
import consulo.application.AccessToken;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.Computable;
import consulo.codeEditor.Editor;
import consulo.container.boot.ContainerPathManager;
import consulo.http.HttpProxyManager;
import consulo.http.HttpRequests;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.io.FileUtilRt;
import consulo.ide.impl.idea.util.net.IOExceptionDialog;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.psi.impl.source.xml.XmlEntityCache;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  @NonNls
  private static final String HTML_MIME = "text/html";
  @NonNls
  private static final String HTTP_PROTOCOL = "http://";
  @NonNls
  private static final String HTTPS_PROTOCOL = "https://";
  @NonNls
  private static final String FTP_PROTOCOL = "ftp://";
  @NonNls
  private static final String EXT_RESOURCES_FOLDER = "extResources";
  private final boolean myForceResultIsValid;

  public FetchExtResourceAction() {
    myForceResultIsValid = false;
  }

  public FetchExtResourceAction(boolean forceResultIsValid) {
    myForceResultIsValid = forceResultIsValid;
  }

  @Override
  protected String getQuickFixKeyId() {
    return "fetch.external.resource";
  }

  @Override
  protected boolean isAcceptableUri(final String uri) {
    return uri.startsWith(HTTP_PROTOCOL) || uri.startsWith(FTP_PROTOCOL) || uri.startsWith(HTTPS_PROTOCOL);
  }

  public static String findUrl(PsiFile file, int offset, String uri) {
    final PsiElement currentElement = file.findElementAt(offset);
    final XmlAttribute attribute = PsiTreeUtil.getParentOfType(currentElement, XmlAttribute.class);

    if (attribute != null) {
      final XmlTag tag = PsiTreeUtil.getParentOfType(currentElement, XmlTag.class);

      if (tag != null) {
        final String prefix = tag.getPrefixByNamespace(XmlUtil.XML_SCHEMA_INSTANCE_URI);
        if (prefix != null) {
          final String attrValue = tag.getAttributeValue(XmlUtil.SCHEMA_LOCATION_ATT, XmlUtil.XML_SCHEMA_INSTANCE_URI);
          if (attrValue != null) {
            final StringTokenizer tokenizer = new StringTokenizer(attrValue);

            while (tokenizer.hasMoreElements()) {
              if (uri.equals(tokenizer.nextToken())) {
                if (!tokenizer.hasMoreElements()) {
                  return uri;
                }
                final String url = tokenizer.nextToken();

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
  protected void doInvoke(@Nonnull final PsiFile file, final int offset, @Nonnull final String uri, final Editor editor) throws IncorrectOperationException {
    final String url = findUrl(file, offset, uri);
    final Project project = file.getProject();

    ProgressManager.getInstance().run(new Task.Backgroundable(project, XmlBundle.message("fetching.resource.title")) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        while (true) {
          try {
            HttpProxyManager.getInstance().prepareURL(url);
            fetchDtd(project, uri, url, indicator);
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                DaemonCodeAnalyzer.getInstance(project).restart(file);
              }
            });
            return;
          } catch (IOException ex) {
            LOG.info(ex);
            @SuppressWarnings("InstanceofCatchParameter") String problemUrl = ex instanceof FetchingResourceIOException ? ((FetchingResourceIOException) ex).url : url;
            String message = XmlBundle.message("error.fetching.title");

            if (!url.equals(problemUrl)) {
              message = XmlBundle.message("error.fetching.dependent.resource.title");
            }

            if (!IOExceptionDialog.showErrorDialog(message, XmlBundle.message("error.fetching.resource", problemUrl))) {
              break; // cancel fetching
            }
          }
        }
      }
    });
  }

  private void fetchDtd(final Project project, final String dtdUrl, final String url, final ProgressIndicator indicator) throws IOException {
    final String extResourcesPath = getExternalResourcesPath();
    final File extResources = new File(extResourcesPath);
    LOG.assertTrue(extResources.mkdirs() || extResources.exists(), extResources);

    final PsiManager psiManager = PsiManager.getInstance(project);
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        @SuppressWarnings("deprecation") final AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(FetchExtResourceAction.class);
        try {
          final String path = FileUtil.toSystemIndependentName(extResources.getAbsolutePath());
          final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
          LOG.assertTrue(vFile != null, path);
        } finally {
          token.finish();
        }
      }
    }, indicator.getModalityState());

    final List<String> downloadedResources = new LinkedList<String>();
    final List<String> resourceUrls = new LinkedList<String>();
    final IOException[] nestedException = new IOException[1];

    try {
      final String resPath = fetchOneFile(indicator, url, project, extResourcesPath, null);
      if (resPath == null) {
        return;
      }
      resourceUrls.add(dtdUrl);
      downloadedResources.add(resPath);

      VirtualFile virtualFile = findFileByPath(resPath, dtdUrl, indicator);

      Set<String> linksToProcess = new HashSet<String>();
      Set<String> processedLinks = new HashSet<String>();
      Map<String, String> baseUrls = new HashMap<String, String>();
      VirtualFile contextFile = virtualFile;
      linksToProcess.addAll(extractEmbeddedFileReferences(virtualFile, null, psiManager, url));

      while (!linksToProcess.isEmpty()) {
        String s = linksToProcess.iterator().next();
        linksToProcess.remove(s);
        processedLinks.add(s);

        final boolean absoluteUrl = s.startsWith(HTTP_PROTOCOL);
        String resourceUrl;
        if (absoluteUrl) {
          resourceUrl = s;
        } else {
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
        } catch (IOException e) {
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

        final Set<String> newLinks = extractEmbeddedFileReferences(virtualFile, contextFile, psiManager, resourceUrl);
        for (String u : newLinks) {
          baseUrls.put(u, resourceUrl);
          if (!processedLinks.contains(u)) {
            linksToProcess.add(u);
          }
        }
      }
    } catch (IOException ex) {
      nestedException[0] = ex;
    }
    if (nestedException[0] != null) {
      cleanup(resourceUrls, downloadedResources);
      throw nestedException[0];
    }
  }

  private static VirtualFile findFileByPath(final String resPath, @Nullable final String dtdUrl, ProgressIndicator indicator) {
    final Ref<VirtualFile> ref = new Ref<VirtualFile>();
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            ref.set(LocalFileSystem.getInstance().refreshAndFindFileByPath(resPath.replace(File.separatorChar, '/')));
            if (dtdUrl != null) {
              ExternalResourceManager.getInstance().addResource(dtdUrl, resPath);
            }
          }
        });
      }
    }, indicator.getModalityState());
    return ref.get();
  }

  public static String getExternalResourcesPath() {
    return ContainerPathManager.get().getSystemPath() + File.separator + EXT_RESOURCES_FOLDER;
  }

  private void cleanup(final List<String> resourceUrls, final List<String> downloadedResources) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
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
                } catch (IOException ignore) {

                }
              }
            }
          }
        });
      }
    });
  }

  @Nullable
  private String fetchOneFile(final ProgressIndicator indicator, final String resourceUrl, final Project project, String extResourcesPath, @Nullable String refname) throws IOException {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        indicator.setText(XmlBundle.message("fetching.progress.indicator", resourceUrl));
      }
    });

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
        final File parent = new File(resPath.substring(0, refNameSlashIndex));
        if (!parent.mkdirs() || !parent.exists()) {
          LOG.warn("Unable to create: " + parent);
        }
      }
    } else {
      resPath += Integer.toHexString(resourceUrl.hashCode()) + "_" + resourceUrl.substring(slashIndex + 1);
    }

    final int lastDoPosInResourceUrl = StringUtil.lastIndexOf(resourceUrl, '.', slashIndex, resourceUrl.length());
    String extension = resourceUrl.substring(lastDoPosInResourceUrl + 1);
    if (lastDoPosInResourceUrl == -1 || FileTypeManager.getInstance().getFileTypeByExtension(extension) == UnknownFileType.INSTANCE) {
      // remote url does not contain file with extension
      extension = result.contentType != null && result.contentType.contains(HTML_MIME) ? HtmlFileType.INSTANCE.getDefaultExtension() : XmlFileType.INSTANCE.getDefaultExtension();
      resPath += "." + extension;
    }

    File res = new File(resPath);

    FileUtil.writeToFile(res, result.bytes);
    return resPath;
  }

  protected boolean resultIsValid(final Project project, ProgressIndicator indicator, final String resourceUrl, FetchResult result) {
    if (myForceResultIsValid) {
      return true;
    }
    if (!ApplicationManager.getApplication().isUnitTestMode() &&
        result.contentType != null &&
        result.contentType.contains(HTML_MIME) &&
        new String(result.bytes).contains("<html")) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showMessageDialog(project, XmlBundle.message("invalid.url.no.xml.file.at.location", resourceUrl), XmlBundle.message("invalid.url.title"), Messages.getErrorIcon());
        }
      }, indicator.getModalityState());
      return false;
    }
    return true;
  }

  private static Set<String> extractEmbeddedFileReferences(XmlFile file, XmlFile context, final String url) {
    final Set<String> result = new LinkedHashSet<String>();
    if (context != null) {
      XmlEntityCache.copyEntityCaches(file, context);
    }

    XmlUtil.processXmlElements(file, new PsiElementProcessor() {
      @Override
      public boolean execute(@Nonnull PsiElement element) {
        if (element instanceof XmlEntityDecl) {
          String candidateName = null;

          for (PsiElement e = element.getLastChild(); e != null; e = e.getPrevSibling()) {
            if (e instanceof XmlAttributeValue && candidateName == null) {
              candidateName = e.getText().substring(1, e.getTextLength() - 1);
            } else if (e instanceof XmlToken &&
                candidateName != null &&
                (((XmlToken) e).getTokenType() == XmlTokenType.XML_DOCTYPE_PUBLIC || ((XmlToken) e).getTokenType() == XmlTokenType.XML_DOCTYPE_SYSTEM)) {
              if (!result.contains(candidateName)) {
                result.add(candidateName);
              }
              break;
            }
          }
        } else if (element instanceof XmlTag) {
          final XmlTag tag = (XmlTag) element;
          String schemaLocation = tag.getAttributeValue(XmlUtil.SCHEMA_LOCATION_ATT);

          if (schemaLocation != null) {
            // processing xsd:import && xsd:include
            final PsiReference[] references = tag.getAttribute(XmlUtil.SCHEMA_LOCATION_ATT).getValueElement().getReferences();
            if (references.length > 0) {
              String extension = FileUtilRt.getExtension(new File(url).getName());
              final String namespace = tag.getAttributeValue("namespace");
              if (namespace != null &&
                  schemaLocation.indexOf('/') == -1 &&
                  !extension.equals(FileUtilRt.getExtension(schemaLocation))) {
                result.add(namespace.substring(0, namespace.lastIndexOf('/') + 1) + schemaLocation);
              } else {
                result.add(schemaLocation);
              }
            }
          } else {
            schemaLocation = tag.getAttributeValue(XmlUtil.SCHEMA_LOCATION_ATT, XmlUtil.XML_SCHEMA_INSTANCE_URI);
            if (schemaLocation != null) {
              final StringTokenizer tokenizer = new StringTokenizer(schemaLocation);

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
      }
    }, true, true);
    return result;
  }

  public static Set<String> extractEmbeddedFileReferences(final VirtualFile vFile, @Nullable final VirtualFile contextVFile, final PsiManager psiManager, final String url) {
    return ApplicationManager.getApplication().runReadAction(new Computable<Set<String>>() {
      @Override
      public Set<String> compute() {
        PsiFile file = psiManager.findFile(vFile);

        if (file instanceof XmlFile) {
          PsiFile contextFile = contextVFile != null ? psiManager.findFile(contextVFile) : null;
          return extractEmbeddedFileReferences((XmlFile) file, contextFile instanceof XmlFile ? (XmlFile) contextFile : null, url);
        }

        return Collections.emptySet();
      }
    });
  }

  protected static class FetchResult {
    byte[] bytes;
    String contentType;
  }

  @Nullable
  private static FetchResult fetchData(final Project project, final String dtdUrl, final ProgressIndicator indicator) throws IOException {
    try {
      return HttpRequests.request(dtdUrl).accept("text/xml,application/xml,text/html,*/*").connect(new HttpRequests.RequestProcessor<FetchResult>() {
        @Override
        public FetchResult process(@Nonnull HttpRequests.Request request) throws IOException {
          FetchResult result = new FetchResult();
          result.bytes = request.readBytes(indicator);
          result.contentType = request.getConnection().getContentType();
          return result;
        }
      });
    } catch (MalformedURLException e) {
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showMessageDialog(project, XmlBundle.message("invalid.url.message", dtdUrl), XmlBundle.message("invalid.url.title"), Messages.getErrorIcon());
          }
        }, indicator.getModalityState());
      }
    }

    return null;
  }
}
