/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.compact;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.LanguageFileType;
import consulo.ui.image.Image;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 01.08.2007
*/
public class RncFileType extends LanguageFileType  {
  public static final String RNC_EXT = "rnc";

  private static final FileType INSTANCE = new RncFileType();

  private RncFileType() {
    super(RngCompactLanguage.INSTANCE);
  }

  @Override
  @Nonnull
  @NonNls
  public String getId() {
    return "RNG Compact";
  }

  @Override
  @Nonnull
  public String getDescription() {
    return "RELAX NG Compact Syntax";
  }

  @Override
  @Nonnull
  @NonNls
  public String getDefaultExtension() {
    return "rnc";
  }

  @Override
  @Nullable
  public Image getIcon() {
    return AllIcons.FileTypes.Text;
  }

  public static FileType getInstance() {
    return INSTANCE;
  }

  public static class Factory extends FileTypeFactory {
    @Override
    public void createFileTypes(@Nonnull FileTypeConsumer fileTypeConsumer) {
      fileTypeConsumer.consume(INSTANCE, RNC_EXT);
    }
  }
}
