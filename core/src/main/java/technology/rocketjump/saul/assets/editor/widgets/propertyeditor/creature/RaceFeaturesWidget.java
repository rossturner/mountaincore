package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.entities.model.physical.creature.features.MeatFeature;
import technology.rocketjump.saul.entities.model.physical.creature.features.RaceFeatures;
import technology.rocketjump.saul.entities.model.physical.creature.features.SkinFeature;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class RaceFeaturesWidget extends VisTable {

	private final RaceFeatures sourceData;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final ItemTypeDictionary itemTypeDictionary;

	public RaceFeaturesWidget(RaceFeatures sourceData, GameMaterialDictionary gameMaterialDictionary, ItemTypeDictionary itemTypeDictionary) {
		this.sourceData = sourceData;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.itemTypeDictionary = itemTypeDictionary;

		//Skin
		VisCheckBox skinCheckbox = new VisCheckBox("Skin");
		VisTable skinWidgetTable = new VisTable();
		if (sourceData.getSkin() != null) {
			skinCheckbox.setChecked(true);
			addSkinFeatureWidgets(skinWidgetTable);
		}
		//TODO: adding listener first, can remove the loaded up sourceData.skin/meat/etc with setting a default one, speak to Ross
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

		//Meat
		VisTable meatWidgetTable = new VisTable();

		VisCheckBox meatCheckbox = new VisCheckBox("Meat");
		if (sourceData.getMeat() != null) {
			meatCheckbox.setChecked(true);
			addMeatFeatureWidgets(meatWidgetTable);
		}
		meatCheckbox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (meatCheckbox.isChecked()) {
					sourceData.setMeat(new MeatFeature());
					addMeatFeatureWidgets(meatWidgetTable);
				} else {
					sourceData.setMeat(null);
					meatWidgetTable.clearChildren();
				}
			}
		});

		this.add(meatCheckbox).left().row();
		this.add(meatWidgetTable).left().row();
	}

	private void addSkinFeatureWidgets(VisTable widgetTable) {
		SkinFeature skin = sourceData.getSkin();

		WidgetBuilder.addSelectField("Skin Material:", "material", gameMaterialDictionary.getAll(), GameMaterial.NULL_MATERIAL, skin, widgetTable);

		widgetTable.add(new VisLabel("Damage reduction: (integer)")).left().colspan(2).row();
		widgetTable.add(new DamageReductionWidget(skin.getDamageReduction())).left().colspan(2).row();
	}


	private void addMeatFeatureWidgets(VisTable widgetTable) {
		MeatFeature meat = sourceData.getMeat();
		//TODO: discuss Item Type with Quantity constraint, guess no point having a null item Type with a quantity? or an itemType with 0 quantity?
		WidgetBuilder.addSelectField("Item Type:", "itemType", itemTypeDictionary.getAll(), null, meat, widgetTable);
		WidgetBuilder.addSelectField("Meat Material:", "material", gameMaterialDictionary.getAll(), GameMaterial.NULL_MATERIAL, meat, widgetTable);
		WidgetBuilder.addIntegerField("Quantity:", "quantity", meat, widgetTable);

	}

}
