/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.util.xml.stubs.builder;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.BinaryFileStubBuilder;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.Stub;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.util.dataholder.Key;
import consulo.util.xml.fastReader.XmlFileHeader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.impl.DomManagerImpl;
import consulo.xml.util.xml.stubs.FileStub;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * Date: 8/2/12
 */
@ExtensionImpl
public class DomStubBuilder implements BinaryFileStubBuilder {

  public final static Key<FileContent> CONTENT_FOR_DOM_STUBS = Key.create("dom stubs content");
  private final static Logger LOG = Logger.getInstance(DomStubBuilder.class);

  @Nonnull
  @Override
  public FileType getFileType() {
    return XmlFileType.INSTANCE;
  }

  @Override
  public boolean acceptsFile(VirtualFile file) {
    FileType fileType = file.getFileType();
    return fileType == XmlFileType.INSTANCE && !ProjectCoreUtil.isProjectOrWorkspaceFile(file, fileType);
  }

  @Override
  public Stub buildStubTree(FileContent fileContent) {
    VirtualFile file = fileContent.getFile();
    Project project = fileContent.getProject();
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (!(psiFile instanceof XmlFile)) {
      return null;
    }

    XmlFile xmlFile = (XmlFile) psiFile;
    try {
      XmlUtil.BUILDING_DOM_STUBS.set(Boolean.TRUE);
      psiFile.putUserData(CONTENT_FOR_DOM_STUBS, fileContent);
      DomFileElement<? extends DomElement> fileElement = DomManager.getDomManager(project).getFileElement(xmlFile);
      if (fileElement == null || !fileElement.getFileDescription().hasStubs()) {
        return null;
      }

      XmlFileHeader header = DomService.getInstance().getXmlFileHeader(xmlFile);
      if (header.getRootTagLocalName() == null) {
        LOG.error("null root tag for " + fileElement + " for " + file);
      }
      FileStub fileStub = new FileStub(header);
      XmlTag rootTag = xmlFile.getRootTag();
      if (rootTag != null) {
        new DomStubBuilderVisitor(DomManagerImpl.getDomManager(project)).visitXmlElement(rootTag, fileStub, 0);
      }
      return fileStub;
    } finally {
      XmlUtil.BUILDING_DOM_STUBS.set(Boolean.FALSE);
      psiFile.putUserData(CONTENT_FOR_DOM_STUBS, null);
    }
  }

  @Override
  public int getStubVersion() {
    int version = 11;
    for (DomFileDescription description : DomFileDescription.EP_NAME.getExtensionList()) {
      version += description.getStubVersion();
    }
    return version;
  }
}
