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

package com.intellij.util.xml.impl;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.xml.*;
import com.intellij.util.xml.*;
import com.intellij.util.xml.structure.DomStructureViewBuilder;
import com.intellij.util.xml.stubs.FileStub;
import com.intellij.util.xml.stubs.builder.DomStubBuilder;
import com.intellij.xml.util.XmlUtil;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UserDataCache;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.ide.impl.idea.util.Function;
import consulo.language.impl.internal.psi.stub.StubTreeLoader;
import consulo.language.internal.PsiFileEx;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ObjectStubTree;
import consulo.language.psi.stub.Stub;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.io.CharSequenceReader;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.fastReader.NanoXmlUtil;
import consulo.util.xml.fastReader.XmlFileHeader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
@Singleton
public class DomServiceImpl extends DomService {
  private static final Key<CachedValue<XmlFileHeader>> ROOT_TAG_NS_KEY = Key.create("rootTag&ns");
  private static final UserDataCache<CachedValue<XmlFileHeader>,XmlFile,Object> ourRootTagCache = new UserDataCache<CachedValue<XmlFileHeader>, XmlFile, Object>() {
    protected CachedValue<XmlFileHeader> compute(final XmlFile file, final Object o) {
      return CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<XmlFileHeader>() {
        public Result<XmlFileHeader> compute() {
          return new Result<XmlFileHeader>(calcXmlFileHeader(file), file);
        }
      }, false);
    }
  };

  @Nonnull
  private static XmlFileHeader calcXmlFileHeader(final XmlFile file) {

    if (file instanceof PsiFileEx && ((PsiFileEx)file).isContentsLoaded() && file.getNode().isParsed()) {
      return computeHeaderByPsi(file);
    }

    if (!XmlUtil.isStubBuilding() && file.getFileType() == XmlFileType.INSTANCE) {
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile instanceof VirtualFileWithId) {
        ObjectStubTree tree = StubTreeLoader.getInstance().readFromVFile(file.getProject(), virtualFile);
        if (tree != null) {
          Stub root = tree.getRoot();
          if (root instanceof FileStub) {
            return ((FileStub)root).getHeader();
          }
        }
      }
    }

    if (!file.isValid()) return XmlFileHeader.EMPTY;

    if (XmlUtil.isStubBuilding() && file.getFileType() == XmlFileType.INSTANCE) {
      FileContent fileContent = file.getUserData(DomStubBuilder.CONTENT_FOR_DOM_STUBS);
      if (fileContent != null) {
        //noinspection IOResourceOpenedButNotSafelyClosed
        return NanoXmlUtil.parseHeader(new CharSequenceReader(fileContent.getContentAsText()));
      }
    }
    return NanoXmlUtil.parseHeaderWithException(file.getViewProvider().getContents());
  }

  private static XmlFileHeader computeHeaderByPsi(XmlFile file) {
    final XmlDocument document = file.getDocument();
    if (document == null) {
      return XmlFileHeader.EMPTY;
    }

    String publicId = null;
    String systemId = null;
    final XmlProlog prolog = document.getProlog();
    if (prolog != null) {
      final XmlDoctype doctype = prolog.getDoctype();
      if (doctype != null) {
        publicId = doctype.getPublicId();
        systemId = doctype.getSystemId();
        if (systemId == null) {
          systemId = doctype.getDtdUri();
        }
      }
    }

    final XmlTag tag = document.getRootTag();
    if (tag == null) {
      return XmlFileHeader.EMPTY;
    }

    String localName = tag.getLocalName();
    if (StringUtil.isNotEmpty(localName)) {
      if (tag.getPrevSibling() instanceof PsiErrorElement) {
        return XmlFileHeader.EMPTY;
      }

      String psiNs = tag.getNamespace();
      return new XmlFileHeader(localName, psiNs == XmlUtil.EMPTY_URI || Comparing.equal(psiNs, systemId) ? null : psiNs, publicId,
                               systemId);
    }
    return XmlFileHeader.EMPTY;
  }

  public ModelMerger createModelMerger() {
    return new ModelMergerImpl();
  }

  @Override
  public <T extends DomElement> DomAnchor<T> createAnchor(T domElement) {
    return DomAnchorImpl.createAnchor(domElement);
  }

  @Nonnull
  public XmlFile getContainingFile(@Nonnull DomElement domElement) {
    if (domElement instanceof DomFileElement) {
      return ((DomFileElement)domElement).getFile();
    }
    return DomManagerImpl.getNotNullHandler(domElement).getFile();
  }

  @Nonnull
  public EvaluatedXmlName getEvaluatedXmlName(@Nonnull final DomElement element) {
    return DomManagerImpl.getNotNullHandler(element).getXmlName();
  }

  @Nonnull
  public XmlFileHeader getXmlFileHeader(XmlFile file) {
    return file.isValid() ? ourRootTagCache.get(ROOT_TAG_NS_KEY, file, null).getValue() : XmlFileHeader.EMPTY;
  }


  public Collection<VirtualFile> getDomFileCandidates(Class<? extends DomElement> description, Project project) {
    return FileBasedIndex.getInstance().getContainingFiles(DomFileIndex.NAME, description.getName(), GlobalSearchScope.allScope(project));
  }

  public <T extends DomElement> List<DomFileElement<T>> getFileElements(final Class<T> clazz, final Project project, @Nullable final GlobalSearchScope scope) {
    final Collection<VirtualFile> list = scope == null ? getDomFileCandidates(clazz, project) : getDomFileCandidates(clazz, project, scope);
    final ArrayList<DomFileElement<T>> result = new ArrayList<DomFileElement<T>>(list.size());
    for (VirtualFile file : list) {
      final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile instanceof XmlFile) {
        final DomFileElement<T> element = DomManager.getDomManager(project).getFileElement((XmlFile)psiFile, clazz);
        if (element != null) {
          result.add(element);
        }
      }
    }

    return result;
  }


  public StructureViewBuilder createSimpleStructureViewBuilder(final XmlFile file, final Function<DomElement, StructureViewMode> modeProvider) {
    return new DomStructureViewBuilder(file, modeProvider);
  }

}
