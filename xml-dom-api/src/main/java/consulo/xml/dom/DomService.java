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

package consulo.xml.dom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Condition;
import consulo.util.xml.fastReader.XmlFileHeader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.language.psi.XmlFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Gregory.Shrago
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class DomService {
    public static DomService getInstance() {
        return Application.get().getService(DomService.class);
    }

    /**
     * @param rootElementClass class of root (file-level) element in DOM model
     * @param project          current project
     * @param scope            search scope
     * @return files containing given root element
     * @see #getFileElements(Class, Project, GlobalSearchScope)
     */
    public Collection<VirtualFile> getDomFileCandidates(Class<? extends DomElement> rootElementClass, Project project, final GlobalSearchScope scope) {
        return ContainerUtil.findAll(getDomFileCandidates(rootElementClass, project), new Condition<VirtualFile>() {
            public boolean value(final VirtualFile file) {
                return scope.contains(file);
            }
        });
    }

    public abstract Collection<VirtualFile> getDomFileCandidates(Class<? extends DomElement> description, Project project);

    /**
     * @param rootElementClass class of root (file-level) element in DOM model
     * @param project          current project
     * @param scope            search scope
     * @return DOM file elements containing given root element
     */
    public abstract <T extends DomElement> List<DomFileElement<T>> getFileElements(Class<T> rootElementClass,
                                                                                   final Project project,
                                                                                   @Nullable GlobalSearchScope scope);

    public abstract ModelMerger createModelMerger();

    public abstract <T extends DomElement> DomAnchor<T> createAnchor(T domElement);

    public abstract XmlFile getContainingFile(DomElement domElement);

    public abstract EvaluatedXmlName getEvaluatedXmlName(DomElement element);

    public abstract XmlFileHeader getXmlFileHeader(XmlFile file);

    public enum StructureViewMode {
        SHOW,
        SHOW_CHILDREN,
        SKIP
    }

    public abstract StructureViewBuilder createSimpleStructureViewBuilder(final XmlFile file, final Function<DomElement, StructureViewMode> modeProvider);

    @Nullable
    public DomTarget getTarget(DomElement element) {
        final GenericDomValue nameElement = element.getGenericInfo().getNameDomElement(element);
        if (nameElement == null) {
            return null;
        }

        return getTarget(element, nameElement);
    }

    @Nullable
    public abstract DomTarget getTarget(DomElement element, GenericDomValue nameElement);
}
