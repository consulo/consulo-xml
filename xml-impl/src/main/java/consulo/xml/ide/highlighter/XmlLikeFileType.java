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
package consulo.xml.ide.highlighter;

import java.nio.charset.Charset;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.project.Project;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.xml.util.XmlUtil;

public abstract class XmlLikeFileType extends LanguageFileType {
  public XmlLikeFileType(Language language) {
    super(language);
  }
  public String getCharset(@Nonnull VirtualFile file, final byte[] content) {
    String charset = XmlUtil.extractXmlEncodingFromProlog(content);
    return charset == null ? CharsetToolkit.UTF8 : charset;
  }

  public Charset extractCharsetFromFileContent(final Project project, @Nullable final VirtualFile file, @Nonnull final CharSequence content) {
    String name = XmlUtil.extractXmlEncodingFromProlog(content);
    Charset charset = CharsetToolkit.forName(name);
    return charset == null ? CharsetToolkit.UTF8_CHARSET : charset;
  }
}
