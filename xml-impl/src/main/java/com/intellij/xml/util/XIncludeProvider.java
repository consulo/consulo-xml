/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.xml.util;

import consulo.xml.ide.highlighter.XmlFileType;
import consulo.language.psi.include.FileIncludeInfo;
import consulo.language.psi.include.FileIncludeProvider;
import consulo.language.psi.stub.FileContent;
import consulo.util.io.Readers;
import consulo.util.lang.CharArrayUtil;
import consulo.util.xml.fastReader.NanoXmlUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
public class XIncludeProvider extends FileIncludeProvider {
  @Nonnull
  @Override
  public String getId() {
    return "XInclude";
  }

  @Override
  public boolean acceptFile(VirtualFile file) {
    return file.getFileType() == XmlFileType.INSTANCE;
  }

  @Override
  public void registerFileTypesUsedForIndexing(@Nonnull Consumer<FileType> fileTypeSink) {
    fileTypeSink.accept(XmlFileType.INSTANCE);
  }

  @Nonnull
  @Override
  public FileIncludeInfo[] getIncludeInfos(FileContent content) {
    CharSequence contentAsText = content.getContentAsText();
    if (CharArrayUtil.indexOf(contentAsText, XmlUtil.XINCLUDE_URI, 0) == -1) {
      return FileIncludeInfo.EMPTY;
    }
    final ArrayList<FileIncludeInfo> infos = new ArrayList<FileIncludeInfo>();
    NanoXmlUtil.parse(Readers.readerFromCharSequence(contentAsText), new NanoXmlUtil.IXMLBuilderAdapter() {

      boolean isXInclude;

      @Override
      public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
        isXInclude = XmlUtil.XINCLUDE_URI.equals(nsURI) && "include".equals(name);
      }

      @Override
      public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception {
        if (isXInclude && "href".equals(key)) {
          infos.add(new FileIncludeInfo(value));
        }
      }

      @Override
      public void endElement(String name, String nsPrefix, String nsURI) throws Exception {
        isXInclude = false;
      }
    });
    return infos.toArray(new FileIncludeInfo[infos.size()]);
  }
}
