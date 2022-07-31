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
package consulo.xml.featureStatistics;

import com.intellij.xml.XmlBundle;
import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.FeatureDescriptor;
import consulo.externalService.statistic.GroupDescriptor;
import consulo.externalService.statistic.ProductivityFeaturesProvider;
import consulo.xml.codeInsight.completion.XmlCompletionContributor;

import java.util.Collections;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlProductivityFeatureProvider extends ProductivityFeaturesProvider {
  public FeatureDescriptor[] getFeatureDescriptors() {
    return new FeatureDescriptor[] { new FeatureDescriptor(XmlCompletionContributor.TAG_NAME_COMPLETION_FEATURE,
                                                           "completion",
                                                           "TagNameCompletion.html",
                                                           XmlBundle.message("tag.name.completion.display.name"),
                                                           0,
                                                           1,
                                                           Collections.<String>emptySet(),
                                                           3,
                                                           this)};
  }

  public GroupDescriptor[] getGroupDescriptors() {
    return new GroupDescriptor[0];
  }
}
