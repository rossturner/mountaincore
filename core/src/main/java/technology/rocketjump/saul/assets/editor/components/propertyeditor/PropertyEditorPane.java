package technology.rocketjump.saul.assets.editor.components.propertyeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.*;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserCallback;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserConfiguration;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.model.EditorAssetSelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.*;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.Sanity;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.assets.entities.item.model.ItemPlacement.BEING_CARRIED;
import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.*;
import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

public class PropertyEditorPane extends VisTable {

	public static final String NOT_APPLICABLE = "(any)";
	public static final String TRUE_COLOUR = "(none/true color)";
	private final NativeFileChooser fileChooser;
	private final EditorStateProvider editorStateProvider;
	private final VisTable editorTable;
	private final VisTable spriteDescriptorsTable;
	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final ProfessionDictionary professionDictionary;

	@Inject
	public PropertyEditorPane(NativeFileChooser fileChooser, EditorStateProvider editorStateProvider, EntityAssetTypeDictionary entityAssetTypeDictionary,
							  ProfessionDictionary professionDictionary) {
		this.fileChooser = fileChooser;
		this.editorStateProvider = editorStateProvider;
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.professionDictionary = professionDictionary;
		editorTable = new VisTable();
		spriteDescriptorsTable = new VisTable();
		VisScrollPane editorScrollPane = new VisScrollPane(editorTable);

		showControlsFor(null);

//		this.setDebug(true);
		this.background("window-bg");
		this.add(new VisLabel("Property Editor")).left().row();
		this.add(editorScrollPane).pad(10).top().row();
		this.add(new VisTable()).expandY();
	}

	public void showControlsFor(Object instance) {
		editorTable.clearChildren();

		if (instance == null) {
			editorTable.add(new VisLabel("Select an item to edit"));
			return;
		}

		switch (instance) {
			case CreatureEntityAsset a -> showEditorControls(a);
			case Race race -> showEditorControls(race);
			case PlantSpecies plantSpecies -> showEditorControls(plantSpecies);
			default ->
					Logger.warn("Not yet implemented: Contrls for " + instance.getClass().getSimpleName() + " in " + getClass().getSimpleName());
		}

	}

	private void showEditorControls(CreatureEntityAsset creatureAsset) {

		VisTextField uniqueNameField = new VisTextField(creatureAsset.getUniqueName());
		uniqueNameField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setUniqueName(uniqueNameField.getText());
			}
		});
		editorTable.add(new VisLabel("Unique name:")).left().row();
		editorTable.add(uniqueNameField).left().fillX().row();

		VisSelectBox<EntityAssetType> assetTypeSelectBox = new VisSelectBox<>();
		assetTypeSelectBox.setItems(orderedArray(entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE)));
		assetTypeSelectBox.setSelected(creatureAsset.getType());
		assetTypeSelectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setType(assetTypeSelectBox.getSelected());
			}
		});
		editorTable.add(new VisLabel("Asset type:")).left().row();
		editorTable.add(assetTypeSelectBox).left().row();

		// TODO default race based on selected entity
//		VisSelectBox<Race> raceSelectBox = new VisSelectBox<>();
//		raceSelectBox.setItems(orderedArray(raceDictionary.getAll()));
//		raceSelectBox.setSelected(creatureAsset.getRace());
//		raceSelectBox.addListener(new ChangeListener() {
//			@Override
//			public void changed(ChangeEvent event, Actor actor) {
//				creatureAsset.setRace(raceSelectBox.getSelected());
//			}
//		});
//		editorTable.add(new VisLabel("Race:")).left().row();
//		editorTable.add(raceSelectBox).left().row();

		VisSelectBox<CreatureBodyShape> bodyShapeSelect = new VisSelectBox<>();
		bodyShapeSelect.setItems(new Array<>(CreatureBodyShape.values()));
		if (creatureAsset.getBodyShape() == null) {
			bodyShapeSelect.setSelected(CreatureBodyShape.ANY);
		} else {
			bodyShapeSelect.setSelected(creatureAsset.getBodyShape());
		}
		bodyShapeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setBodyShape(bodyShapeSelect.getSelected());
			}
		});
		editorTable.add(new VisLabel("Body shape:")).left().row();
		editorTable.add(bodyShapeSelect).left().row();

		VisSelectBox<Gender> genderSelect = new VisSelectBox<>();
		genderSelect.setItems(new Array<>(Gender.values()));
		if (creatureAsset.getGender() == null) {
			genderSelect.setSelected(Gender.ANY);
		} else {
			genderSelect.setSelected(creatureAsset.getGender());
		}
		genderSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setGender(genderSelect.getSelected());
			}
		});
		editorTable.add(new VisLabel("Gender:")).left().row();
		editorTable.add(genderSelect).left().row();

		VisSelectBox<Profession> professionSelect = new VisSelectBox<>();
		professionSelect.setItems(orderedArray(professionDictionary.getAll(), NULL_PROFESSION));
		if (creatureAsset.getProfession() == null) {
			professionSelect.setSelected(NULL_PROFESSION);
		} else {
			professionSelect.setSelected(professionDictionary.getByName(creatureAsset.getProfession()));
		}
		professionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setProfession(professionSelect.getSelected().getName());
			}
		});
		editorTable.add(new VisLabel("Profession:")).left().row();
		editorTable.add(professionSelect).left().row();

		VisSelectBox<String> sanitySelect = new VisSelectBox<>();
		sanitySelect.setItems(orderedArray(
				Arrays.stream(Sanity.values()).map(Sanity::name).collect(Collectors.toList()),
				NOT_APPLICABLE));
		sanitySelect.setSelected(creatureAsset.getSanity() == null ? NOT_APPLICABLE : creatureAsset.getSanity().name());
		sanitySelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String selected = sanitySelect.getSelected();
				creatureAsset.setSanity(selected.equals(NOT_APPLICABLE) ? null : Sanity.valueOf(selected));
			}
		});
		editorTable.add(new VisLabel("Sanity:")).left().row();
		editorTable.add(sanitySelect).left().row();

		if (creatureAsset.getConsciousnessList() == null) {
			creatureAsset.setConsciousnessList(new ArrayList<>());
		}
		if (creatureAsset.getConsciousness() != null && !creatureAsset.getConsciousnessList().contains(creatureAsset.getConsciousness())) {
			creatureAsset.getConsciousnessList().add(creatureAsset.getConsciousness());
			creatureAsset.setConsciousness(null);
		}
		VisLabel consciousnessLabel = new VisLabel("Consciousness (click to show)");
		VisTable consciousnessTable = new VisTable();
		for (Consciousness value : Consciousness.values()) {
			VisCheckBox valueCheckbox = new VisCheckBox(value.name());
			valueCheckbox.setChecked(creatureAsset.getConsciousnessList().contains(value));
			valueCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (valueCheckbox.isChecked()) {
						creatureAsset.getConsciousnessList().add(value);
					} else {
						creatureAsset.getConsciousnessList().remove(value);
					}
				}
			});
			consciousnessTable.add(valueCheckbox).left().padLeft(20).row();
		}
		CollapsibleWidget consciousnessCollapsible = new CollapsibleWidget(consciousnessTable);
		consciousnessCollapsible.setCollapsed(creatureAsset.getConsciousnessList().isEmpty());
		consciousnessLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				consciousnessCollapsible.setCollapsed(!consciousnessCollapsible.isCollapsed());
			}
		});
		editorTable.add(consciousnessLabel).left().row();
		editorTable.add(consciousnessCollapsible).left().row();


		VisLabel tagsLabel = new VisLabel("Tags (click to show)");
		TagsComponent tagsComponent = new TagsComponent(creatureAsset.getTags());
		CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsComponent);
		tagsCollapsible.setCollapsed(creatureAsset.getTags().isEmpty());
		tagsLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
			}
		});
		editorTable.add(tagsLabel).left().expandX().fillX().row();
		editorTable.add(tagsCollapsible).expandX().fillX().left().row();

		showSpriteDescriptorControls(creatureAsset, EntityType.CREATURE);
		editorTable.add(spriteDescriptorsTable).expandX().fillX().left().row();
	}

	private void showSpriteDescriptorControls(EntityAsset entityAsset, EntityType entityType) {
		List<EntityAssetOrientation> orientations = getApplicableOrientations(entityAsset, entityType);

		spriteDescriptorsTable.clearChildren();

		for (EntityAssetOrientation orientation : orientations) {
			SpriteDescriptor spriteDescriptor = entityAsset.getSpriteDescriptors().computeIfAbsent(orientation, a -> new SpriteDescriptor());
			VisTable orientationTable = new VisTable();

			orientationTable.add(new VisLabel(orientation.name())).padBottom(5).left().colspan(2).row();

			VisTextButton browseButton = new VisTextButton("Browse");
			VisTextField filenameField = new VisTextField(spriteDescriptor.getFilename());
			browseButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					fileChooser.chooseFile(buildConfig(), new NativeFileChooserCallback() {
						@Override
						public void onFileChosen(FileHandle file) {
							String filename = file.name();
							spriteDescriptor.setFilename(filename);
							// Need to update sprites if file is already available
							filenameField.setText(filename);
						}

						@Override
						public void onCancellation() {

						}

						@Override
						public void onError(Exception exception) {

						}
					});
				}
			});
			filenameField.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					spriteDescriptor.setFilename(filenameField.getText());
				}
			});
			VisTable rowTable = new VisTable();
			rowTable.add(filenameField).left().expandX().fillX().padRight(5);
			rowTable.add(browseButton);
			orientationTable.add(rowTable).left().expandX().fillX().colspan(2).row();

			VisCheckBox animatedCheckbox = new VisCheckBox("is Animated");
			animatedCheckbox.setChecked(spriteDescriptor.getIsAnimated());
			animatedCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					spriteDescriptor.setIsAnimated(animatedCheckbox.isChecked());
				}
			});
			orientationTable.add(animatedCheckbox).left().colspan(2).row();

			VisLabel coloringLayerLabel = new VisLabel("Coloring layer:");
			VisSelectBox<String> coloringLayerSelect = new VisSelectBox<>();
			Collection<String> applicableLayers = getApplicableColoringLayers(entityType);
			if (spriteDescriptor.getColoringLayer() != null && !applicableLayers.contains(spriteDescriptor.getColoringLayer().name())) {
				Logger.error(spriteDescriptor.getColoringLayer() + " is not included in applicable coloring layers for " + entityType);
			}
			coloringLayerSelect.setItems(orderedArray(applicableLayers, TRUE_COLOUR));
			coloringLayerSelect.setSelected(spriteDescriptor.getColoringLayer() == null ? TRUE_COLOUR : spriteDescriptor.getColoringLayer().name());
			coloringLayerSelect.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (coloringLayerSelect.getSelected().equals(TRUE_COLOUR)) {
						spriteDescriptor.setColoringLayer(null);
					} else {
						spriteDescriptor.setColoringLayer(ColoringLayer.valueOf(coloringLayerSelect.getSelected()));
					}
				}
			});
			orientationTable.add(coloringLayerLabel).left();
			orientationTable.add(coloringLayerSelect).left().row();

			orientationTable.add(new VisLabel("Scale:")).left();
			VisTextField scaleField = new VisTextField(String.valueOf(spriteDescriptor.getScale()));
			scaleField.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					try {
						Float value = Float.valueOf(scaleField.getText());
						if (value != null) {
							spriteDescriptor.setScale(value);
						}
					} catch (NumberFormatException e) {

					}
				}
			});
			orientationTable.add(scaleField).left().row();

			VisCheckBox flipXCheckbox = new VisCheckBox("Flip X");
			flipXCheckbox.setChecked(spriteDescriptor.isFlipX());
			flipXCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					spriteDescriptor.setFlipX(flipXCheckbox.isChecked());
				}
			});
			orientationTable.add(flipXCheckbox).left();
			VisCheckBox flipYCheckbox = new VisCheckBox("Flip Y");
			flipYCheckbox.setChecked(spriteDescriptor.isFlipY());
			flipYCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					spriteDescriptor.setFlipY(flipYCheckbox.isChecked());
				}
			});
			orientationTable.add(flipYCheckbox).left().row();


			orientationTable.add(new OffsetPixelsComponent(spriteDescriptor.getOffsetPixels())).left().colspan(2).row();


			addChildAssetsComponents("Child assets (click to show)", spriteDescriptor.getChildAssets(), entityType, orientationTable);
			addChildAssetsComponents("Attachment points (click to show)", spriteDescriptor.getAttachmentPoints(), entityType, orientationTable);
			addChildAssetsComponents("Parent entity assets (click to show)", spriteDescriptor.getParentEntityAssets(), entityType, orientationTable);

			spriteDescriptorsTable.addSeparator().row();
			spriteDescriptorsTable.add(orientationTable).expandX().fillX().row();
		}

	}

	private void addChildAssetsComponents(String labelText, List<EntityChildAssetDescriptor> childAssets, EntityType entityType, VisTable orientationTable) {
		VisLabel label = new VisLabel(labelText);
		ChildAssetsComponent childAssetsComponent = new ChildAssetsComponent(childAssets,
				entityAssetTypeDictionary.getByEntityType(entityType));
		CollapsibleWidget collapsibleChildAssets = new CollapsibleWidget(childAssetsComponent);
		collapsibleChildAssets.setCollapsed(childAssets.isEmpty());
		label.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				collapsibleChildAssets.setCollapsed(!collapsibleChildAssets.isCollapsed());
			}
		});
		orientationTable.add(label).left().colspan(2).row();
		orientationTable.add(collapsibleChildAssets).padLeft(20).left().expandX().fillX().colspan(2).row();
	}

	private void showEditorControls(Race race) {

	}

	private void showEditorControls(PlantSpecies species) {

	}

	public static <T> Array<T> orderedArray(Collection<T> items) {
		return orderedArray(items, null);
	}

	public static <T> Array<T> orderedArray(Collection<T> items, T nullItem) {
		Array<T> array = new Array<>();
		items.forEach(array::add);
		array.sort(Comparator.comparing(Object::toString));
		if (nullItem != null) {
			array.insert(0, nullItem);
		}
		return array;
	}

	private NativeFileChooserConfiguration buildConfig() {
		EditorAssetSelection assetSelection = editorStateProvider.getState().getAssetSelection();
		NativeFileChooserConfiguration config = new NativeFileChooserConfiguration();
		config.directory = Gdx.files.local(Path.of(assetSelection.getDescriptorsPath()).getParent().toString());
		config.nameFilter = (dir, name) -> name.endsWith("png");
		config.title = "Select sprite file (.png)";
		return config;
	}

	private List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset, EntityType entityType) {
		if (entityAsset instanceof ItemEntityAsset) {
			List<ItemPlacement> itemPlacements = ((ItemEntityAsset) entityAsset).getItemPlacements();
			if (itemPlacements.isEmpty() || itemPlacements.contains(BEING_CARRIED)) {
				return List.of(DOWN, DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT, UP);
			} else {
				return List.of(DOWN);
			}
		} else {
			return switch (entityType) {
				case CREATURE -> List.of(DOWN, DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT, UP);
				case FURNITURE -> List.of(DOWN, LEFT, RIGHT, UP);
				default -> List.of(DOWN);
			};
		}
	}

	private Collection<String> getApplicableColoringLayers(EntityType entityType) {
		List<ColoringLayer> layers = switch (entityType) {
			case CREATURE -> List.of(HAIR_COLOR, SKIN_COLOR, EYE_COLOR, ACCESSORY_COLOR, MARKING_COLOR, BONE_COLOR, OTHER_COLOR);
			case PLANT -> List.of(BRANCHES_COLOR, LEAF_COLOR, FRUIT_COLOR, FLOWER_COLOR, WOOD_COLOR, OTHER_COLOR, VEGETABLE_COLOR);
			// furniture and items and mechanisms under default
			default -> List.of(MISC_COLOR_1, MISC_COLOR_2, MISC_COLOR_3, MISC_COLOR_4, MISC_COLOR_5,
					BONE_COLOR, SEED_COLOR, VEGETABLE_COLOR, CLOTH_COLOR, ROPE_COLOR, EARTH_COLOR, STONE_COLOR,
					ORE_COLOR, GEM_COLOR, METAL_COLOR, WOOD_COLOR, VITRIOL_COLOR, FOODSTUFF_COLOR, LIQUID_COLOR, OTHER_COLOR);
		};

		return layers.stream().map(Enum::name).collect(Collectors.toList());
	}
}
