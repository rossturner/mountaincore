package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.*;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserCallback;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserConfiguration;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.model.EditorAssetSelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.BodyShapesWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.GenderWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.RaceFeaturesWidget;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.*;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviourDictionary;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyStructureDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.materials.GameMaterialDictionary;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.*;
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
	private final BodyStructureDictionary bodyStructureDictionary;
	private final MessageDispatcher messageDispatcher;
	private final CreatureBehaviourDictionary creatureBehaviourDictionary;
	private final ScheduleDictionary scheduleDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	//TODO: discuss refactor to reduce dependencies here
	private final ItemTypeDictionary itemTypeDictionary;

	@Inject
	public PropertyEditorPane(NativeFileChooser fileChooser, EditorStateProvider editorStateProvider, EntityAssetTypeDictionary entityAssetTypeDictionary,
							  ProfessionDictionary professionDictionary, BodyStructureDictionary bodyStructureDictionary,
							  MessageDispatcher messageDispatcher, CreatureBehaviourDictionary creatureBehaviourDictionary,
							  ScheduleDictionary scheduleDictionary, GameMaterialDictionary gameMaterialDictionary, ItemTypeDictionary itemTypeDictionary) {
		this.fileChooser = fileChooser;
		this.editorStateProvider = editorStateProvider;
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.professionDictionary = professionDictionary;
		this.bodyStructureDictionary = bodyStructureDictionary;
		this.messageDispatcher = messageDispatcher;
		this.creatureBehaviourDictionary = creatureBehaviourDictionary;
		this.scheduleDictionary = scheduleDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
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

		try {
			switch (instance) {
				case CreatureEntityAsset a -> showEditorControls(a);
				case Race race -> showEditorControls(race);
				case PlantSpecies plantSpecies -> showEditorControls(plantSpecies);
				default ->
						Logger.warn("Not yet implemented: Contrls for " + instance.getClass().getSimpleName() + " in " + getClass().getSimpleName());
			}
		} catch (Exception e) {
			Logger.error("Unexpected exception setting up controls for " + instance, e);
		}

	}

	private void showEditorControls(Race race) {
		addTextField("Name:", "name", race, editorTable);
		addTextField("I18N key:", "i18nKey", race, editorTable);

		addFloatField("Minimum strength:", "minStrength", race, editorTable);
		addFloatField("Maximum strength:", "maxStrength", race, editorTable);

		addSelectField("Body structure:", "bodyStructure", bodyStructureDictionary.getAll(), null, race, editorTable);

		VisLabel bodyShapesLabel = new VisLabel("Body shapes:");
		VisTable bodyShapesTable = new VisTable();
		bodyShapesTable.add(new VisTable()).width(20).left();
		bodyShapesTable.add(new BodyShapesWidget(race.getBodyShapes())).expandX().fillX().row();
		editorTable.add(bodyShapesLabel).left().colspan(2).row();
		editorTable.add(bodyShapesTable).colspan(2).left().row();

		editorTable.add(new VisLabel("Colors:")).left().colspan(2).row();
		editorTable.add(new ColorsWidget(race.getColors(), getApplicableColoringLayerValues(EntityType.CREATURE),
				EntityType.CREATURE, Path.of(editorStateProvider.getState().getAssetSelection().getDescriptorsPath()), fileChooser, messageDispatcher)).left().colspan(2).row();

		addSelectField("Behaviour:", "behaviourName", creatureBehaviourDictionary.getAllNames(), "", race.getBehaviour(), editorTable);
		addSelectField("Schedule:", "scheduleName", scheduleDictionary.getAllNames(), "", race.getBehaviour(), editorTable);

		editorTable.add(new VisLabel("Needs:")).left();
		VisTable needTable = new VisTable();
		for (EntityNeed need : EntityNeed.values()) {
			VisCheckBox needCheckbox = new VisCheckBox(need.name());
			needCheckbox.setChecked(race.getBehaviour().getNeeds().contains(need));
			needCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (needCheckbox.isChecked()) {
						race.getBehaviour().getNeeds().add(need);
					} else {
						race.getBehaviour().getNeeds().remove(need);
					}
				}
			});
			needTable.add(needCheckbox).left().padLeft(10);
		}
		editorTable.add(needTable).left().row();

		VisTable groupControlsTable = new VisTable();
		VisCheckBox behaviourGroupCheckbox = new VisCheckBox("Behaviour Group:");
		if (race.getBehaviour().getGroup() != null) {
			behaviourGroupCheckbox.setChecked(true);
			addBehaviourGroupControls(race.getBehaviour().getGroup(), groupControlsTable);
		}
		behaviourGroupCheckbox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (behaviourGroupCheckbox.isChecked()) {
					race.getBehaviour().setGroup(new RaceBehaviourGroup());
					addBehaviourGroupControls(race.getBehaviour().getGroup(), groupControlsTable);
				} else {
					groupControlsTable.clearChildren();
					race.getBehaviour().setGroup(null);
				}
			}
		});
		editorTable.add(behaviourGroupCheckbox).left().colspan(2).row();
		editorTable.add(groupControlsTable).left().expandX().colspan(2).row();

		addSelectField("Aggression response:", "aggressionResponse", List.of(AggressionResponse.values()),
				null, race.getBehaviour(), editorTable);

		editorTable.add(new VisLabel("Gender settings:")).left().colspan(2).row();
		editorTable.add(new GenderWidget(race.getGenders())).left().colspan(2).row();


		editorTable.add(new VisLabel("Features:")).left().colspan(2).row();
		editorTable.add(new RaceFeaturesWidget(race.getFeatures(), gameMaterialDictionary, itemTypeDictionary)).left().colspan(2).row();

	}

	private void addBehaviourGroupControls(RaceBehaviourGroup group, VisTable groupControlsTable) {
		try {
			addIntegerField("Behaviour group min size:", "minSize", group, groupControlsTable);
			addIntegerField("Behaviour group max size:", "maxSize", group, groupControlsTable);
		} catch (PropertyReflectionException e) {
			Logger.error("Error creating widgets", e);
		}
	}

	private void showEditorControls(CreatureEntityAsset creatureAsset) throws InvocationTargetException {
		addTextField("Unique name:", "uniqueName", creatureAsset, editorTable);

		addSelectField("Asset type:", "type", entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE), null, creatureAsset, editorTable);

		addSelectField("Body shape:", "bodyShape", List.of(CreatureBodyShape.values()), CreatureBodyShape.ANY, creatureAsset, editorTable);
		addSelectField("Gender:", "gender", List.of(Gender.values()), Gender.ANY, creatureAsset, editorTable);

		List<String> allProfessions = professionDictionary.getAll().stream().map(Profession::getName).collect(Collectors.toList());
		allProfessions.add(0, NULL_PROFESSION.getName());
		addSelectField("Profession:", "profession", allProfessions, NULL_PROFESSION.getName(), creatureAsset, editorTable);

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
		editorTable.add(new VisLabel("Sanity:")).left();
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
		editorTable.add(consciousnessLabel).left().colspan(2).row();
		editorTable.add(consciousnessCollapsible).left().colspan(2).row();


		VisLabel tagsLabel = new VisLabel("Tags (click to show)");
		TagsWidget tagsWidget = new TagsWidget(creatureAsset.getTags());
		CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
		tagsCollapsible.setCollapsed(creatureAsset.getTags().isEmpty());
		tagsLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
			}
		});
		editorTable.add(tagsLabel).left().expandX().fillX().colspan(2).row();
		editorTable.add(tagsCollapsible).expandX().fillX().left().colspan(2).row();

		showSpriteDescriptorControls(creatureAsset, EntityType.CREATURE);
		editorTable.add(spriteDescriptorsTable).expandX().fillX().colspan(2).left().row();
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


			orientationTable.add(new OffsetPixelsWidget(spriteDescriptor.getOffsetPixels())).left().colspan(2).row();


			addChildAssetsWidgets("Child assets (click to show)", spriteDescriptor.getChildAssets(), entityType, orientationTable);
			addChildAssetsWidgets("Attachment points (click to show)", spriteDescriptor.getAttachmentPoints(), entityType, orientationTable);
			addChildAssetsWidgets("Parent entity assets (click to show)", spriteDescriptor.getParentEntityAssets(), entityType, orientationTable);

			spriteDescriptorsTable.addSeparator().row();
			spriteDescriptorsTable.add(orientationTable).expandX().fillX().row();
		}

	}

	private void addChildAssetsWidgets(String labelText, List<EntityChildAssetDescriptor> childAssets, EntityType entityType, VisTable orientationTable) {
		VisLabel label = new VisLabel(labelText);
		ChildAssetsWidget childAssetsWidget = new ChildAssetsWidget(childAssets,
				entityAssetTypeDictionary.getByEntityType(entityType));
		CollapsibleWidget collapsibleChildAssets = new CollapsibleWidget(childAssetsWidget);
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

	private void showEditorControls(PlantSpecies species) {

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

	private List<ColoringLayer> getApplicableColoringLayerValues(EntityType entityType) {
		return switch (entityType) {
			case CREATURE -> List.of(HAIR_COLOR, SKIN_COLOR, EYE_COLOR, ACCESSORY_COLOR, MARKING_COLOR, BONE_COLOR, OTHER_COLOR);
			case PLANT -> List.of(BRANCHES_COLOR, LEAF_COLOR, FRUIT_COLOR, FLOWER_COLOR, WOOD_COLOR, OTHER_COLOR, VEGETABLE_COLOR);
			// furniture and items and mechanisms under default
			default -> List.of(MISC_COLOR_1, MISC_COLOR_2, MISC_COLOR_3, MISC_COLOR_4, MISC_COLOR_5,
					BONE_COLOR, SEED_COLOR, VEGETABLE_COLOR, CLOTH_COLOR, ROPE_COLOR, EARTH_COLOR, STONE_COLOR,
					ORE_COLOR, GEM_COLOR, METAL_COLOR, WOOD_COLOR, VITRIOL_COLOR, FOODSTUFF_COLOR, LIQUID_COLOR, OTHER_COLOR);
		};
	}

	private List<String> getApplicableColoringLayers(EntityType entityType) {
		return getApplicableColoringLayerValues(entityType).stream().map(Enum::name).collect(Collectors.toList());
	}
}
