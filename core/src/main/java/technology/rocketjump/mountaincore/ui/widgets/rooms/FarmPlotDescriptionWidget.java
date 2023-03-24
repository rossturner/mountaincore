package technology.rocketjump.mountaincore.ui.widgets.rooms;

import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.environment.model.Season;
import technology.rocketjump.mountaincore.rooms.components.FarmPlotComponent;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmPlotDescriptionWidget extends Container<Label> {

	private final I18nTranslator i18nTranslator;
	private final Skin skin;
	private PlantSpecies selectedCrop;

	public FarmPlotDescriptionWidget(FarmPlotComponent farmPlotComponent, I18nTranslator i18nTranslator, Skin skin) {
		this.i18nTranslator = i18nTranslator;
		this.skin = skin;
		selectedCrop = farmPlotComponent != null ? farmPlotComponent.getSelectedCrop() : null;
		rebuild();
	}

	private void rebuild() {
		this.clearChildren();
		I18nText text;
		if (selectedCrop == null) {
			text = i18nTranslator.getTranslatedString("GUI.LEAVE_CROP_FALLOW");
		} else {

			List<Season> plantingSeasons = selectedCrop.getSeed().getPlantingSeasons();
			String plantingHintI18nKey = "GUI.CROP_SELECTION.PLANTING_HINT.SEASONS_"+plantingSeasons.size();
			Map<String, I18nString> plantingHintReplacements = new HashMap<>();
			for (int index = 0; index < plantingSeasons.size(); index++) {
				plantingHintReplacements.put("season" + (index + 1), i18nTranslator.getTranslatedString(plantingSeasons.get(index).getI18nKey()));
			}
			I18nText plantingHintText = i18nTranslator.applyReplacements(i18nTranslator.getWord(plantingHintI18nKey), plantingHintReplacements, Gender.ANY);

			Map<String, I18nString> replacements = new HashMap<>();
			replacements.put("planting", plantingHintText);

			if (selectedCrop.getUsageI18nKey() != null) {
				replacements.put("usage", i18nTranslator.getTranslatedString(selectedCrop.getUsageI18nKey()).tidy(true));
			}

			replacements.put("plantDescription", i18nTranslator.getTranslatedString(selectedCrop.getSeed().getSeedMaterial().getI18nKey()));

			text = i18nTranslator.applyReplacements(i18nTranslator.getWord("GUI.CROP_SELECTION.DESCRIPTION"), replacements, Gender.ANY);
		}

		Label label = new Label(text.toString(), skin.get("default-red", Label.LabelStyle.class));
		this.setActor(label);
	}

	private PlantEntityAttributes fakeAttributes(PlantSpecies selectedCrop) {
		return new PlantEntityAttributes(0L, selectedCrop);
	}

	public void cropChanged(PlantSpecies plantSpecies) {
		this.selectedCrop = plantSpecies;
		rebuild();
	}
}
