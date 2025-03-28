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

package consulo.xml.util.xml.highlighting;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.inspection.scheme.event.ProfileChangeAdapter;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiModificationTracker;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.impl.DomApplicationComponent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Singleton
@ServiceImpl
public class DomElementAnnotationsManagerImpl extends DomElementAnnotationsManager {
  public static final Object LOCK = new Object();

  private static final Logger LOG = Logger.getInstance("#DomElementAnnotationsManagerImpl");
  private static final Key<DomElementsProblemsHolderImpl> DOM_PROBLEM_HOLDER_KEY = Key.create("DomProblemHolder");
  private static final Key<CachedValue<Boolean>> CACHED_VALUE_KEY = Key.create("DomProblemHolderCachedValue");
  private final EventDispatcher<DomHighlightingListener> myDispatcher = EventDispatcher.create(DomHighlightingListener.class);

  private static final DomElementsProblemsHolder EMPTY_PROBLEMS_HOLDER = new DomElementsProblemsHolder() {
    @Override
    @Nonnull
    public List<DomElementProblemDescriptor> getProblems(DomElement domElement) {
      return Collections.emptyList();
    }

    @Override
    public List<DomElementProblemDescriptor> getProblems(final DomElement domElement, boolean includeXmlProblems) {
      return Collections.emptyList();
    }

    @Override
    public List<DomElementProblemDescriptor> getProblems(final DomElement domElement,
                                                         final boolean includeXmlProblems,
                                                         final boolean withChildren) {
      return Collections.emptyList();
    }

    @Override
    public List<DomElementProblemDescriptor> getProblems(DomElement domElement,
                                                         final boolean includeXmlProblems,
                                                         final boolean withChildren,
                                                         HighlightSeverity minSeverity) {
      return Collections.emptyList();
    }

    @Override
    public List<DomElementProblemDescriptor> getProblems(DomElement domElement, final boolean withChildren, HighlightSeverity minSeverity) {
      return Collections.emptyList();
    }

    @Override
    public List<DomElementProblemDescriptor> getAllProblems() {
      return Collections.emptyList();
    }

    @Override
    public List<DomElementProblemDescriptor> getAllProblems(@Nonnull DomElementsInspection inspection) {
      return Collections.emptyList();
    }

    @Override
    public boolean isInspectionCompleted(@Nonnull final DomElementsInspection inspectionClass) {
      return false;
    }

  };
  private final ModificationTracker myModificationTracker;
  private final Project myProject;
  private long myModificationCount;

  @Inject
  public DomElementAnnotationsManagerImpl(Project project) {
    myProject = project;
    myModificationTracker = new ModificationTracker() {
      @Override
      public long getModificationCount() {
        return myModificationCount;
      }
    };
    final ProfileChangeAdapter profileChangeAdapter = new ProfileChangeAdapter() {
      @Override
      public void profileActivated(@Nonnull Profile oldProfile, Profile profile) {
        dropAnnotationsCache();
      }

      @Override
      public void profileChanged(Profile profile) {
        dropAnnotationsCache();
      }
    };

    final InspectionProfileManager inspectionProfileManager = InspectionProfileManager.getInstance();
    inspectionProfileManager.addProfileChangeListener(profileChangeAdapter, project);
    Disposer.register(project, new Disposable() {
      @Override
      public void dispose() {
        inspectionProfileManager.removeProfileChangeListener(profileChangeAdapter);
      }
    });
  }

  @Override
  public void dropAnnotationsCache() {
    myModificationCount++;
  }

  public final List<DomElementProblemDescriptor> appendProblems(@Nonnull DomFileElement element,
                                                                @Nonnull DomElementAnnotationHolder annotationHolder,
                                                                Class<? extends DomElementsInspection> inspectionClass) {
    final DomElementAnnotationHolderImpl holderImpl = (DomElementAnnotationHolderImpl)annotationHolder;
    synchronized (LOCK) {
      final DomElementsProblemsHolderImpl holder = _getOrCreateProblemsHolder(element);
      holder.appendProblems(holderImpl, inspectionClass);
    }
    myDispatcher.getMulticaster().highlightingFinished(element);
    return Collections.unmodifiableList(holderImpl);
  }

  private DomElementsProblemsHolderImpl _getOrCreateProblemsHolder(final DomFileElement element) {
    DomElementsProblemsHolderImpl holder;
    final DomElement rootElement = element.getRootElement();
    final XmlTag rootTag = rootElement.getXmlTag();
    if (rootTag == null) {
      return new DomElementsProblemsHolderImpl(element);
    }

    holder = rootTag.getUserData(DOM_PROBLEM_HOLDER_KEY);
    if (isHolderOutdated(element.getFile()) || holder == null) {
      holder = new DomElementsProblemsHolderImpl(element);
      rootTag.putUserData(DOM_PROBLEM_HOLDER_KEY, holder);
      final CachedValue<Boolean> cachedValue =
        CachedValuesManager.getManager(myProject).createCachedValue(new CachedValueProvider<Boolean>() {
          @Override
          public Result<Boolean> compute() {
            return new Result<>(Boolean.FALSE,
																element,
																PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
																myModificationTracker,
																ProjectRootManager.getInstance(myProject));
          }
        }, false);
      cachedValue.getValue();
      element.getFile().putUserData(CACHED_VALUE_KEY, cachedValue);
    }
    return holder;
  }

  public static boolean isHolderUpToDate(DomElement element) {
    synchronized (LOCK) {
      return !isHolderOutdated(DomUtil.getFile(element));
    }
  }

  public static void outdateProblemHolder(final DomElement element) {
    synchronized (LOCK) {
      DomUtil.getFile(element).putUserData(CACHED_VALUE_KEY, null);
    }
  }

  private static boolean isHolderOutdated(final XmlFile file) {
    final CachedValue<Boolean> cachedValue = file.getUserData(CACHED_VALUE_KEY);
    return cachedValue == null || !cachedValue.hasUpToDateValue();
  }

  @Override
  @Nonnull
  public DomElementsProblemsHolder getProblemHolder(DomElement element) {
    if (element == null || !element.isValid()) {
      return EMPTY_PROBLEMS_HOLDER;
    }
    final DomFileElement<DomElement> fileElement = DomUtil.getFileElement(element);

    synchronized (LOCK) {
      final XmlTag tag = fileElement.getRootElement().getXmlTag();
      if (tag != null) {
        final DomElementsProblemsHolder readyHolder = tag.getUserData(DOM_PROBLEM_HOLDER_KEY);
        if (readyHolder != null) {
          return readyHolder;
        }
      }
      return EMPTY_PROBLEMS_HOLDER;
    }
  }

  @Override
  @Nonnull
  public DomElementsProblemsHolder getCachedProblemHolder(DomElement element) {
    return getProblemHolder(element);
  }

  public static void annotate(final DomElement element, final DomElementAnnotationHolder holder, final Class rootClass) {
    final DomElementsAnnotator annotator = DomApplicationComponent.getInstance().getAnnotator(rootClass);
    if (annotator != null) {
      annotator.annotate(element, holder);
    }
  }

  @Override
  public List<ProblemDescriptor> createProblemDescriptors(final InspectionManager manager, DomElementProblemDescriptor problemDescriptor) {
    return ContainerUtil.createMaybeSingletonList(DomElementsHighlightingUtil.createProblemDescriptors(manager, problemDescriptor));
  }

  @Override
  public boolean isHighlightingFinished(final DomElement[] domElements) {
    for (final DomElement domElement : domElements) {
      if (getHighlightStatus(domElement) != DomHighlightStatus.INSPECTIONS_FINISHED) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void addHighlightingListener(DomHighlightingListener listener, Disposable parentDisposable) {
    myDispatcher.addListener(listener, parentDisposable);
  }

  @Override
  public DomHighlightingHelper getHighlightingHelper() {
    return DomHighlightingHelperImpl.INSTANCE;
  }

  @Override
  @Nonnull
  public <T extends DomElement, State> List<DomElementProblemDescriptor> checkFileElement(@Nonnull final DomFileElement<T> domFileElement,
                                                                                          @Nonnull final DomElementsInspection<T, State> inspection,
                                                                                          boolean onTheFly,
                                                                                          State state) {
    final DomElementsProblemsHolder problemHolder = getProblemHolder(domFileElement);
    if (isHolderUpToDate(domFileElement) && problemHolder.isInspectionCompleted(inspection)) {
      return problemHolder.getAllProblems(inspection);
    }

    final DomElementAnnotationHolder holder = new DomElementAnnotationHolderImpl(onTheFly);
    inspection.checkFileElement(domFileElement, holder, state);
    return appendProblems(domFileElement, holder, inspection.getClass());
  }

  public List<DomElementsInspection> getSuitableDomInspections(final DomFileElement fileElement, boolean enabledOnly) {
    Class rootType = fileElement.getRootElementClass();
    final InspectionProfile profile = getInspectionProfile(fileElement);
    final List<DomElementsInspection> inspections = new SmartList<>();
    for (final InspectionToolWrapper toolWrapper : profile.getInspectionTools(fileElement.getFile())) {
      if (!enabledOnly || profile.isToolEnabled(HighlightDisplayKey.find(toolWrapper.getShortName()), fileElement.getFile())) {
        ContainerUtil.addIfNotNull(inspections, getSuitableInspection(toolWrapper.getTool(), rootType));
      }
    }
    return inspections;
  }

  protected InspectionProfile getInspectionProfile(final DomFileElement fileElement) {
    return InspectionProjectProfileManager.getInstance(fileElement.getManager().getProject()).getInspectionProfile();
  }

  @Nullable
  private static DomElementsInspection getSuitableInspection(InspectionTool tool, Class rootType) {
    if (tool instanceof DomElementsInspection) {
      if (((DomElementsInspection)tool).getDomClasses().contains(rootType)) {
        return (DomElementsInspection)tool;
      }
    }
    return null;
  }

  @Nullable
  public <T extends DomElement, State> DomElementsInspection<T, State> getMockInspection(DomFileElement<T> root) {
    if (root.getFileDescription().isAutomaticHighlightingEnabled()) {
      return new MockAnnotatingDomInspection<>(root.getRootElementClass());
    }
    if (getSuitableDomInspections(root, false).isEmpty()) {
      return new MockDomInspection<>(root.getRootElementClass());
    }

    return null;
  }

  private static boolean areInspectionsFinished(DomElementsProblemsHolderImpl holder,
                                                final List<DomElementsInspection> suitableInspections) {
    for (final DomElementsInspection inspection : suitableInspections) {
      if (!holder.isInspectionCompleted(inspection)) {
        return false;
      }
    }
    return true;
  }

  @Nonnull
  public DomHighlightStatus getHighlightStatus(final DomElement element) {
    synchronized (LOCK) {
      final DomFileElement<DomElement> root = DomUtil.getFileElement(element);
      if (!isHolderOutdated(root.getFile())) {
        final DomElementsProblemsHolder holder = getProblemHolder(element);
        if (holder instanceof DomElementsProblemsHolderImpl) {
          DomElementsProblemsHolderImpl holderImpl = (DomElementsProblemsHolderImpl)holder;
          final List<DomElementsInspection> suitableInspections = getSuitableDomInspections(root, true);
          final DomElementsInspection mockInspection = getMockInspection(root);
          final boolean annotatorsFinished = mockInspection == null || holderImpl.isInspectionCompleted(mockInspection);
          final boolean inspectionsFinished = areInspectionsFinished(holderImpl, suitableInspections);
          if (annotatorsFinished) {
            if (suitableInspections.isEmpty() || inspectionsFinished) {
              return DomHighlightStatus.INSPECTIONS_FINISHED;
            }
            return DomHighlightStatus.ANNOTATORS_FINISHED;
          }
        }
      }
      return DomHighlightStatus.NONE;
    }

  }
}
