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
package com.intellij.xml.index;

import consulo.ide.impl.idea.util.NullableFunction;
import consulo.index.io.ID;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.AdditionalIndexedRootsScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class IndexedRelevantResource<K, V extends Comparable> implements Comparable<IndexedRelevantResource<K, V>> {

  public static <K, V extends Comparable> List<IndexedRelevantResource<K, V>> getResources(ID<K, V> indexId,
                                                                                           final K key,
                                                                                           @Nullable final consulo.module.Module module,
                                                                                           @Nonnull Project project,
                                                                                           @Nullable final GlobalSearchScope additionalScope) {

    if (project.isDefault()) return Collections.emptyList();
    final ArrayList<IndexedRelevantResource<K, V>> resources = new ArrayList<IndexedRelevantResource<K, V>>();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    FileBasedIndex.getInstance().processValues(indexId, key, null, new FileBasedIndex.ValueProcessor<V>() {
      public boolean process(VirtualFile file, V value) {
        ResourceRelevance relevance = ResourceRelevance.getRelevance(file, module, fileIndex, additionalScope);
        if (relevance != ResourceRelevance.NONE) {
          resources.add(new IndexedRelevantResource<K, V>(file, key, value, relevance));
        }
        return true;
      }
    }, new AdditionalIndexedRootsScope(GlobalSearchScope.allScope(project)));
    return resources;
  }

  public static <K, V extends Comparable> List<IndexedRelevantResource<K, V>> getAllResources(ID<K, V> indexId,
                                                                                              @Nullable final Module module,
                                                                                              @Nonnull Project project,
                                                                                              @Nullable NullableFunction<List<IndexedRelevantResource<K, V>>, IndexedRelevantResource<K, V>> chooser) {
    ArrayList<IndexedRelevantResource<K, V>> all = new ArrayList<IndexedRelevantResource<K, V>>();
    Collection<K> allKeys = FileBasedIndex.getInstance().getAllKeys(indexId, project);
    for (K key : allKeys) {
      List<IndexedRelevantResource<K, V>> resources = getResources(indexId, key, module, project, null);
      if (!resources.isEmpty()) {
        if (chooser == null) {
          all.add(resources.get(0));
        }
        else {
          IndexedRelevantResource<K, V> resource = chooser.apply(resources);
          if (resource != null) {
            all.add(resource);
          }
        }
      }
    }
    return all;
  }

  private final VirtualFile myFile;
  private final K myKey;
  private final V myValue;
  private final ResourceRelevance myRelevance;

  public IndexedRelevantResource(VirtualFile file, K key, V value, ResourceRelevance relevance) {
    myFile = file;
    myKey = key;
    myValue = value;
    myRelevance = relevance;
  }

  public VirtualFile getFile() {
    return myFile;
  }

  public V getValue() {
    return myValue;
  }

  public ResourceRelevance getRelevance() {
    return myRelevance;
  }

  public int compareTo(IndexedRelevantResource<K, V> o) {
    int i = myRelevance.compareTo(o.getRelevance());
    return i == 0 ? myValue.compareTo(o.getValue()) : i;
  }

  public K getKey() {
    return myKey;
  }

  @Override
  public String toString() {
    return "IndexedRelevantResource{" +
           "myRelevance=" +
           myRelevance +
           ", myKey=" +
           myKey +
           ", myValue=" +
           myValue +
           ", myFile=" +
           myFile +
           '}';
  }
}
