package consulo.xml.codeInsight.daemon.impl.tagTreeHighlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.setting.AdditionalEditorAppearanceSettingProvider;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.Indenter;
import consulo.ui.util.LabeledBuilder;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.localize.XmlLocalize;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2022-08-02
 */
@ExtensionImpl
public class XmlAdditionalEditorAppearanceSettingProvider implements AdditionalEditorAppearanceSettingProvider {
    @Nonnull
    @Override
    public LocalizeValue getLabelName() {
        return XmlLocalize.xmlEditorOptionsMiscTitle();
    }

    @Override
    @RequiredUIAccess
    public void fillProperties(@Nonnull SimpleConfigurableByProperties.PropertyBuilder propertyBuilder, Consumer<Component> consumer) {
        XmlEditorOptions options = XmlEditorOptions.getInstance();

        VerticalLayout layout = VerticalLayout.create();
        consumer.accept(layout);

        CheckBox enableHighlightBox = CheckBox.create(XmlLocalize.settingsEnableHtmlXmlTagTreeHighlighting());
        propertyBuilder.add(enableHighlightBox, options::isTagTreeHighlightingEnabled, options::setTagTreeHighlightingEnabled);
        layout.add(enableHighlightBox);

        IntBox levelToHighlightBox = IntBox.create();
        propertyBuilder.add(levelToHighlightBox, options::getTagTreeHighlightingLevelCount, options::setTagTreeHighlightingLevelCount);
        Component labelLevel = LabeledBuilder.sided(XmlLocalize.settingsLevelsToHighlight(), levelToHighlightBox);
        layout.add(Indenter.indent(labelLevel, 1));

        IntBox opacityBox = IntBox.create();
        opacityBox.setRange(0, 100);
        propertyBuilder.add(opacityBox, options::getTagTreeHighlightingOpacity, options::setTagTreeHighlightingOpacity);
        Component opacityLabel = LabeledBuilder.sided(XmlLocalize.settingsOpacity(), opacityBox);
        layout.add(Indenter.indent(opacityLabel, 1));

        enableHighlightBox.addValueListener(event -> {
            Boolean value = event.getValue();

            labelLevel.setEnabledRecursive(value);
            opacityLabel.setEnabledRecursive(value);
        });
    }
}
