package technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.*;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.jobs.model.SkillType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Singleton
public class CreatureAttributesPane extends AbstractAttributesPane {

	private final SkillDictionary skillDictionary;
	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final ItemEntityFactory itemEntityFactory;
	private final GameContext fakeContext = new GameContext();

	@Inject
	public CreatureAttributesPane(EditorStateProvider editorStateProvider, SkillDictionary skillDictionary,
								  EntityAssetTypeDictionary entityAssetTypeDictionary, CreatureEntityAssetDictionary creatureEntityAssetDictionary,
								  ItemTypeDictionary itemTypeDictionary, ItemEntityFactory itemEntityFactory, MessageDispatcher messageDispatcher) {
		super(editorStateProvider, messageDispatcher);
		this.skillDictionary = skillDictionary;
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.itemEntityFactory = itemEntityFactory;
		fakeContext.setAreaMap(new TiledMap(1, 1, 1, FloorType.NULL_FLOOR, GameMaterial.NULL_MATERIAL));
		fakeContext.setGameClock(new GameClock());
		fakeContext.setRandom(new RandomXS128());
	}

	@Override
	public void reload() {
		this.clearChildren();
		Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) currentEntity.getPhysicalEntityComponent().getAttributes();

		Collection<Gender> genders = attributes.getRace().getGenders().keySet();
		Collection<CreatureBodyShape> bodyShapes = attributes.getRace().getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
		Collection<Consciousness> consciousnesses = Arrays.asList(Consciousness.values());
		Collection<Skill> professions = this.skillDictionary.getAll().stream()
				.filter(skill -> skill.getType().equals(SkillType.PROFESSION) || skill.getType().equals(SkillType.ASSET_OVERRIDE))
				.toList();

		SkillsComponent skillsComponent = currentEntity.getOrCreateComponent(SkillsComponent.class).withNullProfessionActive();

		//Attributes components
		add(WidgetBuilder.selectField("Gender:", attributes.getGender(), genders, null, update(attributes::setGender)));
		add(WidgetBuilder.selectField("Body Shape:", attributes.getBodyShape(), bodyShapes, null, update(attributes::setBodyShape)));
		add(WidgetBuilder.selectField("Consciousness:", attributes.getConsciousness(), consciousnesses, null, update(attributes::setConsciousness)));
		add(WidgetBuilder.selectField("Profession:", skillsComponent.getPrimaryProfession(), professions, null, update(profession -> {
			skillsComponent.clear();
			profession.setType(SkillType.PROFESSION); // bit of a hack in the editor only for the asset override skills
			skillsComponent.setSkillLevel(profession, 50);
		})));
		add(WidgetBuilder.selectField("Sanity:", attributes.getSanity(), List.of(Sanity.values()), Sanity.SANE, update(attributes::setSanity)));
		add(WidgetBuilder.button("New seed (" + attributes.getSeed() + ")", a -> {
			messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorStateProvider.getState().getEntitySelection());
		}));
		row();

		Map<EntityAssetType, EntityAsset> assetMap = currentEntity.getPhysicalEntityComponent().getTypeMap();
		Collection<EntityAssetType> entityAssetTypes = this.entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE);
		for (EntityAssetType type : entityAssetTypes) {
			if (assetMap.containsKey(type)) {
				Skill primaryProfession = currentEntity.getComponent(SkillsComponent.class).getPrimaryProfession();
				List<CreatureEntityAsset> matchingAssets = this.creatureEntityAssetDictionary.getAllMatchingAssets(type, attributes, primaryProfession);
				if (matchingAssets.size() > 1) { //display selection box when more than one
					add(createAssetWidget(type, matchingAssets));
				}
			}
		}
		row();

		//Colours
		for (ColoringLayer coloringLayer : attributes.getColors().keySet()) {
			createColorWidget(coloringLayer, attributes);
		}
		row();

		createEquippedItemWidget();
		createOffHandItemWidget();
	}


	private void createColorWidget(ColoringLayer coloringLayer, CreatureEntityAttributes entityAttributes) {
		Color color = entityAttributes.getColor(coloringLayer);
		TextButton colorButton = new TextButton(HexColors.toHexString(color), new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")));
		colorButton.setColor(color);
		colorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				SpeciesColor speciesColor = entityAttributes.getRace().getColors().get(coloringLayer);
				Color newColor = speciesColor.getColor(new RandomXS128().nextLong());
				entityAttributes.getColors().put(coloringLayer, newColor);
				colorButton.setText(HexColors.toHexString(entityAttributes.getColor(coloringLayer)));
				colorButton.setColor(entityAttributes.getColor(coloringLayer));
			}
		});
		VisTable colorWidget = new VisTable();
		colorWidget.add(new VisLabel(toNiceName(coloringLayer.name()) + ":"));
		colorWidget.add(colorButton);
		add(colorWidget);
	}

	private VisTable createAssetWidget(EntityAssetType assetType, List<CreatureEntityAsset> items) {
		Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
		Map<EntityAssetType, EntityAsset> assetMap = currentEntity.getPhysicalEntityComponent().getTypeMap();
		CreatureEntityAsset initialValue = (CreatureEntityAsset) assetMap.get(assetType);
		return WidgetBuilder.selectField(assetType.name, initialValue, items, null, creatureEntityAsset -> {
			assetMap.put(assetType, creatureEntityAsset);
		});
	}

	private void createEquippedItemWidget() {
		VisTable equippedItemTable = new VisTable();

		VisCheckBox equippedItemCheckbox = new VisCheckBox("Show equipped item");
		equippedItemCheckbox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor
			) {
				Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
				EquippedItemComponent equippedItemComponent = currentEntity.getOrCreateComponent(EquippedItemComponent.class);
				if (equippedItemCheckbox.isChecked()) {
					List<ItemType> itemTypes = itemTypeDictionary.getAll();
					equippedItemTable.add(WidgetBuilder.selectField("", itemTypes.get(fakeContext.getRandom().nextInt(itemTypes.size())),
							itemTypes, null, itemType -> {
								Entity itemEntity = itemEntityFactory.createByItemType(itemType, fakeContext, false, Faction.SETTLEMENT);
								equippedItemComponent.clearMainHandItem();
								equippedItemComponent.setMainHandItem(itemEntity, currentEntity, messageDispatcher);
							}));
				} else {
					equippedItemComponent.clearMainHandItem();
					equippedItemTable.clearChildren();
					equippedItemTable.add(equippedItemCheckbox);
				}
			}
		});
		equippedItemTable.add(equippedItemCheckbox);

		this.add(equippedItemTable).colspan(3).row();
	}


	private void createOffHandItemWidget() {
		VisTable offHandItemTable = new VisTable();

		VisCheckBox equippedItemCheckbox = new VisCheckBox("Show off-hand item");
		equippedItemCheckbox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor
			) {
				Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
				EquippedItemComponent equippedItemComponent = currentEntity.getOrCreateComponent(EquippedItemComponent.class);
				if (equippedItemCheckbox.isChecked()) {
					List<ItemType> itemTypes = itemTypeDictionary.getAll().stream()
							.filter(itemType -> ItemHoldPosition.LEFT_HAND.equals(itemType.getHoldPosition()))
							.toList();
					offHandItemTable.add(WidgetBuilder.selectField("", itemTypes.get(fakeContext.getRandom().nextInt(itemTypes.size())),
							itemTypes, null, itemType -> {
								Entity itemEntity = itemEntityFactory.createByItemType(itemType, fakeContext, false, Faction.SETTLEMENT);
								equippedItemComponent.clearOffHandItem();
								equippedItemComponent.setOffHandItem(itemEntity, currentEntity, messageDispatcher);
							}));
				} else {
					equippedItemComponent.clearOffHandItem();
					offHandItemTable.clearChildren();
					offHandItemTable.add(equippedItemCheckbox);
				}
			}
		});
		offHandItemTable.add(equippedItemCheckbox);

		this.add(offHandItemTable).colspan(3).row();
	}


	private String toNiceName(String value) {
		return WordUtils.capitalizeFully(value, '_');
	}
}
