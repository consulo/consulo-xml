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
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.OptionsBundle;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.xml.editor.XmlHighlighterColors;
import consulo.xml.ide.highlighter.XmlFileHighlighter;

import javax.annotation.Nonnull;
import java.util.Map;

@ExtensionImpl
public class XMLColorsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = new AttributesDescriptor[] {
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.prologue"), XmlHighlighterColors.XML_PROLOGUE),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.comment"), XmlHighlighterColors.XML_COMMENT),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.tag"), XmlHighlighterColors.XML_TAG),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.tag.name"), XmlHighlighterColors.XML_TAG_NAME),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.attribute.name"), XmlHighlighterColors.XML_ATTRIBUTE_NAME),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.attribute.value"), XmlHighlighterColors.XML_ATTRIBUTE_VALUE),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.tag.data"), XmlHighlighterColors.XML_TAG_DATA),
    new AttributesDescriptor(OptionsBundle.message("options.xml.attribute.descriptor.descriptor.entity,reference"), XmlHighlighterColors.XML_ENTITY_REFERENCE),
  };

  @Nonnull
  public String getDisplayName() {
    return OptionsBundle.message("options.xml.display.name");
  }

  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;                       
  }

  @Nonnull
  public SyntaxHighlighter getHighlighter() {
    return new XmlFileHighlighter();
  }

  @Nonnull
  public String getDemoText() {
    return "<?xml version='1.0' encoding='ISO-8859-1'  ?>\n" +
           "<!DOCTYPE index>\n" +
           "<!-- Some xml example -->\n" +
           "<index version=\"1.0\">\n" +
           "   <name>Main Index</name>\n" +
           "   <indexitem text=\"rename\" target=\"refactoring.rename\"/>\n" +
           "   <indexitem text=\"move\" target=\"refactoring.move\"/>\n" +
           "   <indexitem text=\"migrate\" target=\"refactoring.migrate\"/>\n" +
           "   <indexitem text=\"usage search\" target=\"find.findUsages\"/>\n&amp; &#x00B7;" +
           "   <indexitem text=\"project\" target=\"project.management\"/>\n" +
           "</index>";
  }

  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }
}