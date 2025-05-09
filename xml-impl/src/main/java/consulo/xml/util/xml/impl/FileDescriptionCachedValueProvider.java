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
import consulo.application.ApplicationManager;
import consulo.language.psi.PsiFileEx;
import consulo.language.psi.stub.ObjectStubTree;
import consulo.language.psi.stub.StubTreeLoader;
import consulo.language.sem.SemElement;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.function.Condition;
import consulo.util.xml.fastReader.XmlFileHeader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.events.DomEvent;
import consulo.xml.util.xml.stubs.FileStub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
@SuppressWarnings({
    "HardCodedStringLiteral",
    "StringConcatenationInsideStringBufferAppend"
})
class FileDescriptionCachedValueProvider<T extends DomElement> implements SemElement {

  private final XmlFile myXmlFile;
  private volatile boolean myComputed;
  private volatile DomFileElementImpl<T> myLastResult;
  private final MyCondition myCondition = new MyCondition();

  private final DomManagerImpl myDomManager;
  private final DomService myDomService;

  public FileDescriptionCachedValueProvider(final DomManagerImpl domManager, final XmlFile xmlFile) {
    myDomManager = domManager;
    myXmlFile = xmlFile;
    myDomService = DomService.getInstance();
  }

  @Nullable
  public final DomFileElementImpl<T> getFileElement() {
    if (myComputed) {
      return myLastResult;
    }

    DomFileElementImpl<T> result = _computeFileElement(false, myDomService.getXmlFileHeader(myXmlFile), null);

    synchronized (myCondition) {
      if (myComputed) {
        return myLastResult;
      }

      myLastResult = result;
      WeakReference<DomFileElementImpl> ref = result != null ? new WeakReference<DomFileElementImpl>(result) : null;
      myXmlFile.putUserData(DomManagerImpl.CACHED_FILE_ELEMENT, ref);
      myComputed = true;
      return result;
    }
  }

  @Nullable
  private DomFileElementImpl<T> _computeFileElement(final boolean fireEvents, @Nonnull final XmlFileHeader rootTagName, @Nullable StringBuilder sb) {
    if (sb != null) {
      sb.append(rootTagName).append("\n");
    }

    if (!myXmlFile.isValid()) {
      return null;
    }
    if (sb != null) {
      sb.append("File is valid\n");
    }

    final DomFileDescription<T> description = findFileDescription(rootTagName, sb);

    final DomFileElementImpl oldValue = getLastValue();
    if (sb != null) {
      sb.append("last " + oldValue + "\n");
    }
    final List<DomEvent> events = fireEvents ? new SmartList<DomEvent>() : Collections.<DomEvent>emptyList();
    if (oldValue != null) {
      if (fireEvents) {
        events.add(new DomEvent(oldValue, false));
      }
    }

    if (description == null) {
      return null;
    }

    final Class<T> rootElementClass = description.getRootElementClass();
    final XmlName xmlName = DomImplUtil.createXmlName(description.getRootTagName(), rootElementClass, null);
    assert xmlName != null;
    final EvaluatedXmlNameImpl rootTagName1 = EvaluatedXmlNameImpl.createEvaluatedXmlName(xmlName, xmlName.getNamespaceKey(), false);

    VirtualFile file = myXmlFile.getVirtualFile();
    FileStub stub = null;
    if (description.hasStubs() && file instanceof VirtualFileWithId && !isFileParsed()) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
      if (!XmlUtil.isStubBuilding()) {
        ObjectStubTree stubTree = StubTreeLoader.getInstance().readOrBuild(myXmlFile.getProject(), file, myXmlFile);
        if (stubTree != null) {
          stub = (FileStub) stubTree.getRoot();
        }
      }
    }

    DomFileElementImpl<T> result = new DomFileElementImpl<T>(myXmlFile, rootElementClass, rootTagName1, myDomManager, description, stub);
    if (sb != null) {
      sb.append("success " + result + "\n");
    }

    if (fireEvents) {
      events.add(new DomEvent(result, true));
    }
    return result;
  }

  private boolean isFileParsed() {
    return myXmlFile instanceof PsiFileEx && ((PsiFileEx) myXmlFile).isContentsLoaded();
  }

  @Nullable
  private DomFileDescription<T> findFileDescription(final XmlFileHeader rootTagName, @Nullable StringBuilder sb) {
    final DomFileDescription<T> mockDescription = myXmlFile.getUserData(DomManagerImpl.MOCK_DESCRIPTION);
    if (mockDescription != null) {
      return mockDescription;
    }

    if (sb != null) {
      sb.append("no mock\n");
    }

    final XmlFile originalFile = (XmlFile) myXmlFile.getOriginalFile();
    if (sb != null) {
      sb.append("original: " + originalFile + "\n");
    }
    if (!originalFile.equals(myXmlFile)) {
      final FileDescriptionCachedValueProvider<T> provider = myDomManager.getOrCreateCachedValueProvider(originalFile);
      final DomFileElementImpl<T> element = provider.getFileElement();
      if (sb != null) {
        sb.append("originalDom " + element + "\n");
      }
      return element == null ? null : element.getFileDescription();
    }

    //noinspection unchecked
    final Set<DomFileDescription> namedDescriptions = myDomManager.getFileDescriptions(rootTagName.getRootTagLocalName());
    if (sb != null) {
      sb.append("named " + new HashSet<DomFileDescription>(namedDescriptions) + "\n");
    }
    DomFileDescription<T> description = ContainerUtil.find(namedDescriptions, myCondition);
    if (description == null) {
      final Set<DomFileDescription> unnamed = myDomManager.getAcceptingOtherRootTagNameDescriptions();
      description = ContainerUtil.find(unnamed, myCondition);
    }
    if (sb != null) {
      sb.append("found " + description + "\n");
    }
    return description;
  }

  @Nullable
  final DomFileElementImpl<T> getLastValue() {
    return myLastResult;
  }

  public String getFileElementWithLogging() {
    final XmlFileHeader rootTagName = myDomService.getXmlFileHeader(myXmlFile);
    final StringBuilder log = new StringBuilder();
    myLastResult = _computeFileElement(false, rootTagName, log);
    return log.toString();
  }

  private class MyCondition implements Condition<DomFileDescription> {
    @Override
    public boolean value(final DomFileDescription description) {
      return description.isMyFile(myXmlFile);
    }
  }

}
