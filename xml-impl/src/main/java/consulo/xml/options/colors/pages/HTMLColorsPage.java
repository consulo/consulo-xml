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
package consulo.xml.options.colors.pages;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.OptionsBundle;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.xml.codeInsight.daemon.impl.tagTreeHighlighting.XmlTagTreeHighlightingColors;
import consulo.xml.editor.XmlHighlighterColors;
import consulo.xml.ide.highlighter.HtmlFileHighlighter;

import javax.annotation.Nonnull;
import java.util.Map;

@ExtensionImpl
public class HTMLColorsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = new AttributesDescriptor[] {
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.code"), XmlHighlighterColors.HTML_CODE),
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.comment"), XmlHighlighterColors.HTML_COMMENT),
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.tag"), XmlHighlighterColors.HTML_TAG),
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.tag.name"), XmlHighlighterColors.HTML_TAG_NAME),
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.attribute.name"), XmlHighlighterColors.HTML_ATTRIBUTE_NAME),
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.attribute.value"), XmlHighlighterColors.HTML_ATTRIBUTE_VALUE),
    new AttributesDescriptor(OptionsBundle.message("options.html.attribute.descriptor.entity.reference"), XmlHighlighterColors.HTML_ENTITY_REFERENCE),
  };

  @Nonnull
  public String getDisplayName() {
    return OptionsBundle.message("options.html.display.name");
  }

  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    // todo: make preview for it

    final EditorColorKey[] colorKeys = XmlTagTreeHighlightingColors.getColorKeys();
    final ColorDescriptor[] colorDescriptors = new ColorDescriptor[colorKeys.length];

    for (int i = 0; i < colorDescriptors.length; i++) {
      colorDescriptors[i] = new ColorDescriptor(OptionsBundle.message("options.html.attribute.descriptor.tag.tree", i + 1),
                                                colorKeys[i], ColorDescriptor.Kind.BACKGROUND);
    }

    return colorDescriptors;
  }

  @Nonnull
  public SyntaxHighlighter getHighlighter() {
    return new HtmlFileHighlighter();
  }

  @Nonnull
  public String getDemoText() {
    return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n" +
           "<!--\n" +
           "*        Sample comment\n" +
           "-->\n" +
           "<HTML>\n" +
           "<head>\n" +
           "<title>" + Application.get().getName() + "</title>\n" +
           "</head>\n" +
           "<body>\n" +
           "<h1>" + Application.get().getName() + "</h1>\n" +
           "<p><br><b><IMG border=0 height=12 src=\"images/hg.gif\" width=18 >\n" +
           "What is " + Application.get().getName() + "? &#x00B7; &Alpha; </b><br><br>\n" +
           "</body>\n" +
           "</html>";
  }

  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }
}