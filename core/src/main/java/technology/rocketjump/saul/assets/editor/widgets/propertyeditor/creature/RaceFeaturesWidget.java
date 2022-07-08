package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.entities.model.physical.creature.features.RaceFeatures;
import technology.rocketjump.saul.entities.model.physical.creature.features.SkinFeature;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;

public class RaceFeaturesWidget extends VisTable {

	private final RaceFeatures sourceData;
	private final GameMaterialDictionary gameMaterialDictionary;

	public RaceFeaturesWidget(RaceFeatures sourceData, GameMaterialDictionary gameMaterialDictionary) {
		this.sourceData = sourceData;
		this.gameMaterialDictionary = gameMaterialDictionary;

		VisCheckBox skinCheckbox = new VisCheckBox("Skin");
		VisTable skinWidgetTable = new VisTable();
		if (sourceData.getSkin() != null) {
			skinCheckbox.setChecked(true);
			addSkinFeatureWidgets(skinWidgetTable);
		}
		skinCheckbox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (skinCheckbox.isChecked()) {
					sourceData.setSkin(new SkinFeature());
					addSkinFeatureWidgets(skinWidgetTable);
				} else {
					skinWidgetTable.clearChildren();
					sourceData.setSkin(null);
				}
			}
		});
		this.add(skinCheckbox).left().row();
		this.add(skinWidgetTable).left().row();


	}

	private void addSkinFeatureWidgets(VisTable widgetTable) {
		VisSelectBox<GameMaterial> materialSelect = new VisSelectBox<>();
		Array<GameMaterial> allMaterials = orderedArray(gameMaterialDictionary.getAll());
		allMaterials.add(GameMaterial.NULL_MATERIAL);
		materialSelect.setItems(allMaterials);
		if (sourceData.getSkin().getSkinMaterial() == null) {
			materialSelect.setSelected(GameMaterial.NULL_MATERIAL);
		} else {
			materialSelect.setSelected(sourceData.getSkin().getSkinMaterial());
		}
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				GameMaterial selected = materialSelect.getSelected();
				if (selected.equals(GameMaterial.NULL_MATERIAL)) {
					sourceData.getSkin().setSkinMaterialName(null);
					sourceData.getSkin().setSkinMaterial(null);
				} else {
					sourceData.getSkin().setSkinMaterialName(selected.getMaterialName());
					sourceData.getSkin().setSkinMaterial(selected);
				}
			}
		});
		widgetTable.add(new VisLabel("Skin Material:")).left();
		widgetTable.add(materialSelect).left().row();

		widgetTable.add(new VisLabel("Damage reduction: (integer)")).left().colspan(2).row();
		widgetTable.add(new DamageReductionWidget(sourceData.getSkin().getDamageReduction())).left().colspan(2).row();

	}
}
