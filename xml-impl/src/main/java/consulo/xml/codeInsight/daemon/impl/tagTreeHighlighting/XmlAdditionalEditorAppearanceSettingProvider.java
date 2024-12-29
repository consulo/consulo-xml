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

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XmlAdditionalEditorAppearanceSettingProvider implements AdditionalEditorAppearanceSettingProvider
{
	@Nonnull
	@Override
	public LocalizeValue getLabelName()
	{
		return LocalizeValue.localizeTODO("XML/HTML");
	}

	@Override
	@RequiredUIAccess
	public void fillProperties(@Nonnull SimpleConfigurableByProperties.PropertyBuilder propertyBuilder, Consumer<Component> consumer)
	{
		XmlEditorOptions options = XmlEditorOptions.getInstance();

		VerticalLayout layout = VerticalLayout.create();
		consumer.accept(layout);

		CheckBox enableHighlightBox = CheckBox.create(LocalizeValue.localizeTODO("Enable HTML/XML tag tree highlighting"));
		propertyBuilder.add(enableHighlightBox, options::isTagTreeHighlightingEnabled, options::setTagTreeHighlightingEnabled);
		layout.add(enableHighlightBox);

		IntBox levelToHighlightBox = IntBox.create();
		propertyBuilder.add(levelToHighlightBox, options::getTagTreeHighlightingLevelCount, options::setTagTreeHighlightingLevelCount);
		Component labelLevel = LabeledBuilder.sided(LocalizeValue.localizeTODO("Levels to highlight:"), levelToHighlightBox);
		layout.add(Indenter.indent(labelLevel, 1));

		IntBox opacityBox = IntBox.create();
		opacityBox.setRange(0, 100);
		propertyBuilder.add(opacityBox, options::getTagTreeHighlightingOpacity, options::setTagTreeHighlightingOpacity);
		Component opacityLabel = LabeledBuilder.sided(LocalizeValue.localizeTODO("Opacity (%):"), opacityBox);
		layout.add(Indenter.indent(opacityLabel, 1));

		enableHighlightBox.addValueListener(event ->
		{
			Boolean value = event.getValue();

			labelLevel.setEnabledRecursive(value);
			opacityLabel.setEnabledRecursive(value);
		});
	}
}
