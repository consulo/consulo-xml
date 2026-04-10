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
package consulo.xml.util.xml.impl;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UserDataCache;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFileEx;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ObjectStubTree;
import consulo.language.psi.stub.StubTreeLoader;
import consulo.project.Project;
import consulo.util.io.CharSequenceReader;
import consulo.util.lang.StringUtil;
import consulo.util.xml.fastReader.NanoXmlUtil;
import consulo.util.xml.fastReader.XmlFileHeader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.xml.dom.*;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.psi.*;
import consulo.xml.util.xml.DomFileIndex;
import consulo.xml.util.xml.DomTargetImpl;
import consulo.xml.util.xml.ModelMergerImpl;
import consulo.xml.util.xml.structure.DomStructureViewBuilder;
import consulo.xml.util.xml.stubs.FileStub;
import consulo.xml.util.xml.stubs.builder.DomStubBuilder;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Gregory.Shrago
 */
@Singleton
@ServiceImpl
public class DomServiceImpl extends DomService {
    private static final UserDataCache<CachedValue<XmlFileHeader>, XmlFile, Object> ROOT_TAG_CACHE =
        new UserDataCache<CachedValue<XmlFileHeader>, XmlFile, Object>("rootTag&ns") {
            @Override
            @RequiredReadAction
            protected CachedValue<XmlFileHeader> compute(XmlFile file, Object o) {
                return CachedValuesManager.getManager(file.getProject())
                    .createCachedValue(() -> new CachedValueProvider.Result<>(calcXmlFileHeader(file), file), false);
            }
        };

    @RequiredReadAction
    private static XmlFileHeader calcXmlFileHeader(XmlFile file) {
        if (file instanceof PsiFileEx fileEx && fileEx.isContentsLoaded() && file.getNode().isParsed()) {
            return computeHeaderByPsi(file);
        }

        if (!XmlUtil.isStubBuilding() && file.getFileType() == XmlFileType.INSTANCE) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile instanceof VirtualFileWithId) {
                ObjectStubTree tree = StubTreeLoader.getInstance().readFromVFile(file.getProject(), virtualFile);
                if (tree != null && tree.getRoot() instanceof FileStub rootFileStub) {
                    return rootFileStub.getHeader();
                }
            }
        }

        if (!file.isValid()) {
            return XmlFileHeader.EMPTY;
        }

        if (XmlUtil.isStubBuilding() && file.getFileType() == XmlFileType.INSTANCE) {
            FileContent fileContent = file.getUserData(DomStubBuilder.CONTENT_FOR_DOM_STUBS);
            if (fileContent != null) {
                //noinspection IOResourceOpenedButNotSafelyClosed
                return NanoXmlUtil.parseHeader(new CharSequenceReader(fileContent.getContentAsText()));
            }
        }
        return NanoXmlUtil.parseHeaderWithException(file.getViewProvider().getContents());
    }

    @RequiredReadAction
    private static XmlFileHeader computeHeaderByPsi(XmlFile file) {
        XmlDocument document = file.getDocument();
        if (document == null) {
            return XmlFileHeader.EMPTY;
        }

        String publicId = null;
        String systemId = null;
        XmlProlog prolog = document.getProlog();
        if (prolog != null) {
            XmlDoctype doctype = prolog.getDoctype();
            if (doctype != null) {
                publicId = doctype.getPublicId();
                systemId = doctype.getSystemId();
                if (systemId == null) {
                    systemId = doctype.getDtdUri();
                }
            }
        }

        XmlTag tag = document.getRootTag();
        if (tag == null) {
            return XmlFileHeader.EMPTY;
        }

        String localName = tag.getLocalName();
        if (StringUtil.isNotEmpty(localName)) {
            if (tag.getPrevSibling() instanceof PsiErrorElement) {
                return XmlFileHeader.EMPTY;
            }

            String psiNs = tag.getNamespace();
            return new XmlFileHeader(
                localName,
                psiNs == XmlUtil.EMPTY_URI || Objects.equals(psiNs, systemId) ? null : psiNs,
                publicId,
                systemId
            );
        }
        return XmlFileHeader.EMPTY;
    }

    @Override
    public ModelMerger createModelMerger() {
        return new ModelMergerImpl();
    }

    @Override
    public <T extends DomElement> DomAnchor<T> createAnchor(T domElement) {
        return DomAnchorImpl.createAnchor(domElement);
    }

    @Override
    public XmlFile getContainingFile(DomElement domElement) {
        if (domElement instanceof DomFileElement domFileElement) {
            return domFileElement.getFile();
        }
        return DomManagerImpl.getNotNullHandler(domElement).getFile();
    }

    @Override
    public EvaluatedXmlName getEvaluatedXmlName(DomElement element) {
        return DomManagerImpl.getNotNullHandler(element).getXmlName();
    }

    @Override
    @RequiredReadAction
    public XmlFileHeader getXmlFileHeader(XmlFile file) {
        return file.isValid() ? ROOT_TAG_CACHE.get(file, null).getValue() : XmlFileHeader.EMPTY;
    }

    @Override
    public Collection<VirtualFile> getDomFileCandidates(Class<? extends DomElement> description, Project project) {
        return FileBasedIndex.getInstance().getContainingFiles(DomFileIndex.NAME, description.getName(), GlobalSearchScope.allScope(project));
    }

    @Override
    @RequiredReadAction
    public <T extends DomElement> List<DomFileElement<T>> getFileElements(Class<T> clazz, Project project, @Nullable GlobalSearchScope scope) {
        Collection<VirtualFile> list = scope == null ? getDomFileCandidates(clazz, project) : getDomFileCandidates(clazz, project, scope);
        List<DomFileElement<T>> result = new ArrayList<>(list.size());
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile file : list) {
            if (psiManager.findFile(file) instanceof XmlFile xmlFile) {
                DomFileElement<T> element = DomManager.getDomManager(project).getFileElement(xmlFile, clazz);
                if (element != null) {
                    result.add(element);
                }
            }
        }

        return result;
    }

    @Override
    public StructureViewBuilder createSimpleStructureViewBuilder(XmlFile file, Function<DomElement, StructureViewMode> modeProvider) {
        return new DomStructureViewBuilder(file, modeProvider);
    }

    @Override
    public @Nullable DomTarget getTarget(DomElement element, GenericDomValue nameElement) {
        return DomTargetImpl.getTarget(element, nameElement);
    }
}
