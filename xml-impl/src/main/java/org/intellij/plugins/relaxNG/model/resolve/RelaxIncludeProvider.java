package org.intellij.plugins.relaxNG.model.resolve;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.language.psi.include.FileIncludeInfo;
import consulo.language.psi.include.FileIncludeProvider;
import consulo.language.psi.stub.FileContent;
import consulo.util.io.Readers;
import consulo.util.lang.CharArrayUtil;
import consulo.util.xml.fastReader.NanoXmlUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.compact.psi.RncElement;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncInclude;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.function.Consumer;

/*
* User: sweinreuter
* Date: 09.06.2010
*/
@ExtensionImpl
public class RelaxIncludeProvider extends FileIncludeProvider {
  @Nonnull
  @Override
  public String getId() {
    return "relax-ng";
  }

  @Override
  public boolean acceptFile(VirtualFile file) {
    final FileType type = file.getFileType();
    return type == XmlFileType.INSTANCE || type == RncFileType.getInstance();
  }

  @Override
  public void registerFileTypesUsedForIndexing(@Nonnull Consumer<FileType> fileTypeSink) {
    fileTypeSink.accept(XmlFileType.INSTANCE);
    fileTypeSink.accept(RncFileType.getInstance());
  }

  @Nonnull
  @Override
  public FileIncludeInfo[] getIncludeInfos(FileContent content) {
    final ArrayList<FileIncludeInfo> infos;

    if (content.getFileType() == XmlFileType.INSTANCE) {
      CharSequence inputDataContentAsText = content.getContentAsText();
      if (CharArrayUtil.indexOf(inputDataContentAsText, ApplicationLoader.RNG_NAMESPACE, 0) == -1) return FileIncludeInfo.EMPTY;
      infos = new ArrayList<>();
      NanoXmlUtil.parse(Readers.readerFromCharSequence(content.getContentAsText()), new RngBuilderAdapter(infos));
    } else if (content.getFileType() == RncFileType.getInstance()) {
      infos = new ArrayList<>();
      content.getPsiFile().acceptChildren(new RncElementVisitor() {                      
        @Override
        public void visitElement(RncElement element) {
          element.acceptChildren(this);
        }

        @Override
        public void visitInclude(RncInclude include) {
          final String path = include.getFileReference();
          if (path != null) {
            infos.add(new FileIncludeInfo(path));
          }
        }
      });
    } else {
      return FileIncludeInfo.EMPTY;
    }
    return infos.toArray(new FileIncludeInfo[infos.size()]);
  }

  private static class RngBuilderAdapter extends NanoXmlUtil.IXMLBuilderAdapter {
    boolean isRNG;
    boolean isInclude;
    private final ArrayList<FileIncludeInfo> myInfos;

    public RngBuilderAdapter(ArrayList<FileIncludeInfo> infos) {
      myInfos = infos;
    }

    @Override
    public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
      boolean isRngTag = ApplicationLoader.RNG_NAMESPACE.equals(nsURI);
      if (!isRNG) { // analyzing start tag
        if (!isRngTag) {
          stop();
        } else {
          isRNG = true;
        }
      }
      isInclude = isRngTag && "include".equals(name);
    }

    @Override
    public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception {
      if (isInclude && "href".equals(key)) {
        myInfos.add(new FileIncludeInfo(value));
      }
    }

    @Override
    public void endElement(String name, String nsPrefix, String nsURI) throws Exception {
      isInclude = false;
    }
  }
}
