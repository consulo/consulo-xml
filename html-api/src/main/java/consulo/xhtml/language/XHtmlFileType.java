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
package consulo.xhtml.language;

import consulo.application.AllIcons;
import consulo.html.language.HtmlFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.xml.localize.XmlLocalize;


public class XHtmlFileType extends HtmlFileType {
  public static final XHtmlFileType INSTANCE = new XHtmlFileType();

  private XHtmlFileType() {
    super(XHTMLLanguage.INSTANCE);
  }

  public String getId() {
    return "XHTML";
  }

  public LocalizeValue getDescription() {
    return XmlLocalize.filetypeDescriptionXhtml();
  }

  public String getDefaultExtension() {
    return "xhtml";
  }

  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Xhtml;
  }
}
