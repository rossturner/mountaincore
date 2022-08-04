package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.*;
import technology.rocketjump.saul.assets.editor.model.ColorPickerMessage;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WeaponInfoWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.features.*;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.ReflectionUtils;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.rendering.utils.HexColors;

public class RaceFeaturesWidget extends VisTable {

	private abstract static class CheckBoxGroup {
		private final VisCheckBox checkBox;

		public CheckBoxGroup(VisTable parentTable, String checkBoxName, RaceFeatures sourceData, String propertyName) {
			this.checkBox = new VisCheckBox(checkBoxName);
			VisTable childWidgetTable = new VisTable();
			if (ReflectionUtils.getProperty(sourceData, propertyName) != null) {
				this.checkBox.setChecked(true);
				initChildWidgets(childWidgetTable, sourceData);
			}
			this.checkBox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (checkBox.isChecked()) {
						initData(sourceData);
						initChildWidgets(childWidgetTable, sourceData);
					} else {
						ReflectionUtils.setProperty(sourceData, propertyName, null);
						childWidgetTable.clearChildren();
					}
				}
			});
			parentTable.add(this.checkBox).left().row();
			parentTable.add(childWidgetTable).left().row();
		}

		abstract void initData(RaceFeatures sourceData);

		abstract void initChildWidgets(VisTable childContainer, RaceFeatures sourceData);
	}

	public RaceFeaturesWidget(RaceFeatures sourceData, GameMaterialDictionary gameMaterialDictionary, ItemTypeDictionary itemTypeDictionary,
							  MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary,
							  ParticleEffectTypeDictionary particleEffectTypeDictionary) {

		new CheckBoxGroup(this, "Defense", sourceData, "defense") {
			@Override
			void initData(RaceFeatures sourceData) {
				sourceData.setDefense(new DefenseInfo());
			}

			@Override
			void initChildWidgets(VisTable childContainer, RaceFeatures sourceData) {
				DefenseInfo defenseInfo = sourceData.getDefense();

				WidgetBuilder.addIntegerField("Max Defense Points:", "maxDefensePoints", defenseInfo, childContainer);
				WidgetBuilder.addIntegerField("Max Defense Regained/Round:", "maxDefenseRegainedPerRound", defenseInfo, childContainer);

				childContainer.add(new VisLabel("Damage reduction: (integer)")).left().colspan(2).row();
				childContainer.add(new DamageReductionWidget(defenseInfo.getDamageReduction())).left().colspan(2).row();
			}
		};


		new CheckBoxGroup(this, "Unarmed Weapon", sourceData, "unarmedWeapon") {
			@Override
			void initData(RaceFeatures sourceData) {
				sourceData.setUnarmedWeapon(new WeaponInfo());
			}

			@Override
			void initChildWidgets(VisTable childContainer, RaceFeatures sourceData) {
				WeaponInfo unarmedWeapon = sourceData.getUnarmedWeapon();
				childContainer.add(new WeaponInfoWidget(unarmedWeapon, soundAssetDictionary, particleEffectTypeDictionary)).left().colspan(2).row();
			}
		};

		new CheckBoxGroup(this, "Skin", sourceData, "skin") {

			@Override
			void initData(RaceFeatures sourceData) {
				sourceData.setSkin(new SkinFeature());
			}

			@Override
			void initChildWidgets(VisTable childContainer, RaceFeatures sourceData) {
				SkinFeature skin = sourceData.getSkin();

				WidgetBuilder.addSelectField("Skin Material:", "material", gameMaterialDictionary.getAll(), GameMaterial.NULL_MATERIAL, skin, childContainer);
			}
		};

		new CheckBoxGroup(this, "Meat", sourceData, "meat") {

			@Override
			void initData(RaceFeatures sourceData) {
				sourceData.setMeat(new MeatFeature());
			}

			@Override
			void initChildWidgets(VisTable childContainer, RaceFeatures sourceData) {
				MeatFeature meat = sourceData.getMeat();
				WidgetBuilder.addSelectField("Item Type:", "itemType", itemTypeDictionary.getAll(), null, meat, childContainer);
				WidgetBuilder.addSelectField("Meat Material:", "material", gameMaterialDictionary.getAll(), GameMaterial.NULL_MATERIAL, meat, childContainer);
				WidgetBuilder.addIntegerField("Quantity:", "quantity", meat, childContainer);
			}
		};

		new CheckBoxGroup(this, "Bone", sourceData, "bones") {

			@Override
			void initData(RaceFeatures sourceData) {
				sourceData.setBones(new BonesFeature());
			}

			@Override
			void initChildWidgets(VisTable childContainer, RaceFeatures sourceData) {
				BonesFeature bones = sourceData.getBones();
				WidgetBuilder.addSelectField("Bone Material:", "material", gameMaterialDictionary.getAll(), GameMaterial.NULL_MATERIAL, bones, childContainer);
			}
		};

		new CheckBoxGroup(this, "Blood", sourceData, "blood") {
			@Override
			void initData(RaceFeatures sourceData) {
				sourceData.setBlood(new BloodFeature());
			}

			@Override
			void initChildWidgets(VisTable childContainer, RaceFeatures sourceData) {
				BloodFeature blood = sourceData.getBlood();

				String colorCode = blood.getColorCode();
				VisTextField colorCodeField = new VisTextField(colorCode);
				VisTextField.VisTextFieldStyle colorCodeStyle = new VisTextField.VisTextFieldStyle(colorCodeField.getStyle());
				colorCodeStyle.fontColor = blood.getColor();
				colorCodeField.setStyle(colorCodeStyle);

				colorCodeField.setText(colorCode);
				colorCodeField.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						Color validatedColor = HexColors.get(colorCodeField.getText());
						if (validatedColor != null) {
							blood.setColorCode(colorCodeField.getText());
							colorCodeStyle.fontColor = blood.getColor();
						}
					}
				});
				childContainer.add(colorCodeField).expandX().fillX();

				VisTextButton colorPickerButton = new VisTextButton("Picker");
				colorPickerButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_COLOR_PICKER,
								new ColorPickerMessage(blood.getColorCode(), (color) -> {
									blood.setColorCode(HexColors.toHexString(color));
									colorCodeStyle.fontColor = color;
									colorCodeField.setText(blood.getColorCode());
								}));
					}
				});
				childContainer.add(colorPickerButton).row();


			}
		};

	}


}
