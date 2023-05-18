package technology.rocketjump.mountaincore.ui.views.debug;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.combat.CombatMessageHandler;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.NeedsComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.mountaincore.entities.factories.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.*;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.Body;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.BodyPartDamageLevel;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.BodyStructure;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.mountaincore.environment.WeatherManager;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.invasions.InvasionDefinitionDictionary;
import technology.rocketjump.mountaincore.invasions.model.InvasionDefinition;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.TileExploration;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.CreatureDeathMessage;
import technology.rocketjump.mountaincore.messaging.types.DebugMessage;
import technology.rocketjump.mountaincore.messaging.types.ParticleRequestMessage;
import technology.rocketjump.mountaincore.messaging.types.PipeConstructionMessage;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.settlement.ImmigrationManager;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.views.GuiView;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static technology.rocketjump.mountaincore.entities.model.EntityType.*;

/**
 * As this is intended for dev/debug use, it is not translatable or moddable
 */
@Singleton
public class DebugGuiView implements GuiView, GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final Skin skin;
	private final Label titleLabel;
	private final SelectBox<DebugAction> actionSelect;
	private final SelectBox<ItemType> itemTypeSelect;
	private final ItemTypeDictionary itemTypeDictionary;
	private final SelectBox<GameMaterial> materialSelect;
	private final GameMaterialDictionary materialDictionary;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final PlantEntityAttributesFactory plantEntityAttributesFactory;
	private final PlantEntityFactory plantEntityFactory;
	private final VehicleEntityAttributesFactory vehicleEntityAttributesFactory;
	private final VehicleEntityFactory vehicleEntityFactory;
	private final CombatMessageHandler combatMessageHandler;
	private final SelectBox<Race> raceSelect;
	private final SelectBox<String> plantSpeciesSelect;
	private final SelectBox<Integer> plantSpeciesGrowthStageSelect;
	private final SelectBox<String> vehicleTypeSelect;
	private final SelectBox<EntityNeed> needSelect;
	private final SelectBox<Integer> needValueSelect;
	private final SelectBox<String> bodyPartSelect;
	private final Map<String, BodyPart> stringToBodyPart = new TreeMap<>();
	private final ItemEntityFactory itemEntityFactory;
	private final SettlerFactory settlerFactory;
	private final VehicleTypeDictionary vehicleTypeDictionary;
	private final WeatherManager weatherManager;
	private final ImmigrationManager immigrationManager;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final InvasionDefinitionDictionary invasionDefinitionDictionary;
	private Table layoutTable;
	private GameContext gameContext;

	private boolean displayed = false;

	private DebugAction currentAction = DebugAction.NONE;
	private ItemType itemTypeToSpawn;
	private VehicleType vehicleTypeToSpawn;
	private GameMaterial selectedMaterial = GameMaterial.NULL_MATERIAL;

	@Inject
	public DebugGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary,
						ItemEntityAttributesFactory itemEntityAttributesFactory,
						CreatureEntityFactory creatureEntityFactory, CreatureEntityAttributesFactory creatureEntityAttributesFactory,
						RaceDictionary raceDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
						PlantEntityAttributesFactory plantEntityAttributesFactory, PlantEntityFactory plantEntityFactory,
						VehicleEntityAttributesFactory vehicleEntityAttributesFactory, VehicleEntityFactory vehicleEntityFactory, CombatMessageHandler combatMessageHandler, ItemEntityFactory itemEntityFactory, SettlerFactory settlerFactory,
						VehicleTypeDictionary vehicleTypeDictionary, WeatherManager weatherManager,
						ImmigrationManager immigrationManager, ParticleEffectTypeDictionary particleEffectTypeDictionary,
						InvasionDefinitionDictionary invasionDefinitionDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.skin = guiSkinRepository.getMenuSkin();
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.plantEntityAttributesFactory = plantEntityAttributesFactory;
		this.plantEntityFactory = plantEntityFactory;
		this.vehicleEntityAttributesFactory = vehicleEntityAttributesFactory;
		this.vehicleEntityFactory = vehicleEntityFactory;
		this.combatMessageHandler = combatMessageHandler;
		this.itemEntityFactory = itemEntityFactory;
		this.settlerFactory = settlerFactory;
		this.vehicleTypeDictionary = vehicleTypeDictionary;
		this.weatherManager = weatherManager;
		this.immigrationManager = immigrationManager;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		this.invasionDefinitionDictionary = invasionDefinitionDictionary;

		layoutTable = new Table(skin);

		this.titleLabel = new Label("Debug Menu - press middle mouse button to trigger action", skin.get("white_text", Label.LabelStyle.class));

		this.actionSelect =  new SelectBox<>(skin);
		this.actionSelect.setItems(DebugAction.values());
		this.actionSelect.setSelected(currentAction);
		actionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				currentAction = actionSelect.getSelected();
				resetMaterialSelect();
				update();
			}
		});

		this.itemTypeSelect = new SelectBox<>(skin);
		Array<ItemType> itemTypes = new Array<>();
		for (ItemType itemType : this.itemTypeDictionary.getAll()) {
			itemTypes.add(itemType);
		}
		itemTypeToSpawn = itemTypes.get(0);
		itemTypeSelect.setItems(itemTypes);
		itemTypeSelect.setSelected(itemTypeToSpawn);
		itemTypeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				ItemType selectedType = itemTypeSelect.getSelected();
				itemTypeToSpawn = selectedType;
				resetMaterialSelect();
				update();
			}
		});

		this.materialSelect = new SelectBox<>(skin);
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				selectedMaterial = materialSelect.getSelected();
				update();
			}
		});

		this.raceSelect = new SelectBox<>(skin);
		raceSelect.setItems(raceDictionary.getAll().stream().filter(race -> !"Dwarf".equals(race.getName())).toArray(Race[]::new));

		this.needSelect = new SelectBox<>(skin);
		this.needSelect.setItems(EntityNeed.values());

		this.plantSpeciesSelect = new SelectBox<>(skin);
		this.plantSpeciesGrowthStageSelect = new SelectBox<>(skin);

		this.plantSpeciesSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				List<PlantSpeciesGrowthStage> growthStages = plantSpeciesDictionary.getByName(plantSpeciesSelect.getSelected()).getGrowthStages();
				plantSpeciesGrowthStageSelect.setItems(IntStream.range(0, growthStages.size()).boxed().toArray(Integer[]::new));
			}
		});
		this.plantSpeciesSelect.setItems(plantSpeciesDictionary.getAll().stream().map(PlantSpecies::getSpeciesName).toArray(String[]::new));

		this.vehicleTypeSelect = new SelectBox<>(skin);
		this.vehicleTypeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				vehicleTypeToSpawn = DebugGuiView.this.vehicleTypeDictionary.getByName(vehicleTypeSelect.getSelected());
			}
		});
		Array<String> vehicleTypes = new Array<>();
		for (VehicleType vehicleType : this.vehicleTypeDictionary.getAll()) {
			if (!vehicleType.equals(VehicleTypeDictionary.NULL_TYPE)) {
				vehicleTypes.add(vehicleType.getName());
			}
		}
		vehicleTypeSelect.setItems(vehicleTypes);
		vehicleTypeSelect.setSelected(vehicleTypes.get(0));

		Integer[] needValues = IntStream.rangeClosed((int) NeedsComponent.MIN_NEED_VALUE, (int) NeedsComponent.MAX_NEED_VALUE)
				.filter(x -> x % 10 == 0)
				.boxed()
				.toArray(Integer[]::new);
		this.needValueSelect = new SelectBox<>(skin);
		this.needValueSelect.setItems(needValues);


		for (Race race : raceDictionary.getAll()) {
			BodyStructure bodyStructure = race.getBodyStructure();
			Body body = new Body(bodyStructure);
			for (BodyPart bodyPart : body.getAllWorkingBodyParts()) {
				this.stringToBodyPart.put(race.getName() + "-" + bodyPart.toString(), bodyPart);
			}
		}

		this.bodyPartSelect = new SelectBox<>(skin);
		this.bodyPartSelect.setItems(stringToBodyPart.keySet().toArray(String[]::new));

		messageDispatcher.addListener(this, MessageType.TOGGLE_DEBUG_VIEW);
		messageDispatcher.addListener(this, MessageType.DEBUG_MESSAGE);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TOGGLE_DEBUG_VIEW: {
				this.displayed = !this.displayed;
				update();
				return true;
			}
			case MessageType.DEBUG_MESSAGE: {
				if (GlobalSettings.DEV_MODE) {
					DebugMessage message = (DebugMessage) msg.extraInfo;
					MapTile tile = gameContext.getAreaMap().getTile(message.getWorldPosition());

					if (tile != null) {
						handleDebugAction(tile, message.getWorldPosition());
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	private void handleDebugAction(MapTile tile, Vector2 worldPosition) {
		switch (currentAction) {
			case SPAWN_ITEM: {

				List<Entity> itemsInTile = tile.getEntities().stream().filter(e -> e.getType().equals(ITEM)).toList();

				Optional<Entity> existingItemOfType = itemsInTile.stream()
						.filter(e -> e.getType().equals(ITEM) &&
								((ItemEntityAttributes) e.getPhysicalEntityComponent().getAttributes()).getItemType().equals(itemTypeToSpawn))
						.findAny();

				if (existingItemOfType.isPresent()) {
					ItemEntityAttributes existingAttributes = (ItemEntityAttributes) existingItemOfType.get().getPhysicalEntityComponent().getAttributes();
					if (existingAttributes.getQuantity() < existingAttributes.getItemType().getMaxStackSize()) {
						existingAttributes.setQuantity(existingAttributes.getQuantity() + 1);
						messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, existingItemOfType.get());
					}
				} else if (itemsInTile.isEmpty()) {
					List<GameMaterial> materials = new ArrayList<>();
					for (GameMaterialType materialType : itemTypeToSpawn.getMaterialTypes()) {
						if (materialType.equals(selectedMaterial.getMaterialType())) {
							materials.add(selectedMaterial);
						} else {
							List<GameMaterial> materialsForType = materialDictionary.getByType(materialType);
							materials.add(materialsForType.get(gameContext.getRandom().nextInt(materialsForType.size())));
						}
					}

					ItemEntityAttributes attributes = itemEntityAttributesFactory.createItemAttributes(itemTypeToSpawn, 1, materials);
					itemEntityFactory.create(attributes, tile.getTilePosition(), true, gameContext, Faction.SETTLEMENT);
				} else {
					Logger.warn("Blocked spawning of item in tile that already contains an item");
				}
				break;
			}
			case SPAWN_PLANT: {

				List<Entity> plantsInTile = tile.getEntities().stream().filter(e -> e.getType().equals(PLANT)).collect(Collectors.toList());

				if (plantsInTile.isEmpty()) {
					PlantEntityAttributes attributes = plantEntityAttributesFactory.createBySpeciesName(plantSpeciesSelect.getSelected());
					attributes.setGrowthStageCursor(plantSpeciesGrowthStageSelect.getSelected());
					Entity entity = plantEntityFactory.create(attributes, tile.getTilePosition(), gameContext);
					messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
				} else {
					Logger.warn("Blocked spawning of plant in tile that already contains a plant");
				}
				break;
			}
			case SPAWN_VEHICLE: {
				VehicleEntityAttributes attributes = vehicleEntityAttributesFactory.create(vehicleTypeToSpawn);
				vehicleEntityFactory.create(attributes, tile.getTilePosition(), gameContext, Faction.SETTLEMENT);
				break;
			}
			case SPAWN_CREATURE:{
				CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(raceSelect.getSelected());
				Faction faction;
				if (raceSelect.getSelected().getMapPlacement() == CreatureMapPlacement.CAVE_MONSTER) {
					faction = Faction.MONSTERS;
				} else {
					faction = Faction.WILD_ANIMALS;
				}
				creatureEntityFactory.create(attributes, worldPosition, EntityAssetOrientation.DOWN.toVector2(), gameContext, faction);
				break;
			}
			case SPAWN_SETTLER: {
				settlerFactory.create(worldPosition, new SkillsComponent().withNullProfessionActive(), gameContext, true);
				break;
			}
			case KILL_CREATURE: {
				for (Entity entity : tile.getEntities()) {
					if (entity.getType().equals(CREATURE) && !(entity.getBehaviourComponent() instanceof CorpseBehaviour)) {
						messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(entity, DeathReason.UNKNOWN, null));
						break;
					}
					if (entity.getType().equals(FURNITURE)) {
						InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
						for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
							if (inventoryEntry.entity.getType().equals(CREATURE) && !(inventoryEntry.entity.getBehaviourComponent() instanceof CorpseBehaviour)) {
								messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(inventoryEntry.entity, DeathReason.UNKNOWN, null));
								break;
							}
						}

					}
				}
				break;
			}
			case DESTROY_BODY_PART: {
				for (Entity entity : tile.getEntities()) {
					BodyPart bodyPart = stringToBodyPart.get(bodyPartSelect.getSelected());
					EntityAttributes attributes = entity.getPhysicalEntityComponent().getAttributes();
					if (attributes instanceof CreatureEntityAttributes creatureAttributes) {
						creatureAttributes.getBody().setDamage(bodyPart, BodyPartDamageLevel.Destroyed);
						combatMessageHandler.bodyPartDestroyed(bodyPart, creatureAttributes.getBody(), entity, null);
					}
				}
				break;
			}
			case CHANGE_CREATURE_NEED: {
				for (Entity entity : tile.getEntities()) {
					NeedsComponent needs = entity.getComponent(NeedsComponent.class);
					if (entity.getType().equals(CREATURE) && needs != null) {
						needs.setValue(needSelect.getSelected(), needValueSelect.getSelected());
						break;
					}
				}
				break;
			}
			case DESTROY_ENTITY: {
				tile.getEntities().stream().findFirst().ifPresent(entity -> {
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entity);
				});
				break;
			}
			case TRIGGER_FIRE: {
				messageDispatcher.dispatchMessage(MessageType.START_FIRE_IN_TILE, tile);
				break;
			}
			case TRIGGER_LIGHTNING: {
				weatherManager.triggerStrikeAt(tile);
				break;
			}
			case TRIGGER_NEXT_WEATHER: {
				weatherManager.triggerNextWeather();
				break;
			}
			case TRIGGER_NEXT_SEASON: {
				gameContext.getGameClock().nextSeason(messageDispatcher);
				break;
			}
			case TRIGGER_IMMIGRATION: {
				gameContext.getSettlementState().setImmigrantsDue(7);
				immigrationManager.triggerImmigration();
				break;
			}
			case TRIGGER_YEAR_END: {
				gameContext.getGameClock().forceYearChange(messageDispatcher);
				break;
			}
			case TOGGLE_CHANNEL: {
				if (!tile.hasWall()) {
					if (tile.hasChannel()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_CHANNEL, tile.getTilePosition());
					} else {
						messageDispatcher.dispatchMessage(MessageType.ADD_CHANNEL, tile.getTilePosition());
					}
				}
				break;
			}
			case TRIGGER_BREAKDOWN: {
				tile.getEntities().stream().filter(Entity::isSettler).findAny()
						.ifPresent(entity -> entity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.CAUSE_BREAKDOWN));
				break;
			}
			case TOGGLE_PIPE: {
				if (!tile.hasWall()) {
					if (tile.hasPipe()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_PIPE, tile.getTilePosition());
					} else {
						messageDispatcher.dispatchMessage(MessageType.ADD_PIPE, new PipeConstructionMessage(
								tile.getTilePosition(), selectedMaterial));
					}
				}
				break;
			}
			case UNCOVER_UNEXPLORED_AREA: {
				if (tile.getExploration().equals(TileExploration.UNEXPLORED)) {
					messageDispatcher.dispatchMessage(MessageType.FLOOD_FILL_EXPLORATION, tile.getTilePosition());
				}
				break;
			}
			case TRIGGER_TEST_EFFECT: {
				tile.getEntities().stream().filter(e -> e.getType().equals(CREATURE))
						.findAny()
						.ifPresent(entity -> {
							ParticleEffectType effectType = particleEffectTypeDictionary.getByName("Claw slash");
							messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(effectType, Optional.of(entity),
									Optional.empty(), (p) -> {}));
						});
				break;
			}
			case PRETEND_ATTACKED_BY_NEARBY_CREATURE: {
				tile.getEntities().stream().filter(e -> e.getType().equals(CREATURE))
						.findAny()
						.ifPresent(entity -> {
							Entity otherNearbyCreature = null;
							for (int x = -4; x <= 4; x++) {
								for (int y = -4; y <= 4; y++) {
									MapTile otherTile = gameContext.getAreaMap().getTile(tile.getTileX() + x, tile.getTileY() + y);
									if (otherTile != null) {
										Optional<Entity> foundEntity = otherTile.getEntities().stream()
												.filter(e -> e.getType().equals(CREATURE) && e.getId() != entity.getId())
												.findAny();
										if (foundEntity.isPresent()) {
											MemoryComponent memoryComponent = entity.getOrCreateComponent(MemoryComponent.class);
											Memory attackedByCreatureMemory = new Memory(MemoryType.ATTACKED_BY_CREATURE, gameContext.getGameClock());
											attackedByCreatureMemory.setRelatedEntityId(foundEntity.get().getId());
											memoryComponent.addShortTerm(attackedByCreatureMemory, gameContext.getGameClock());
										}
									}
								}
							}
						});
				break;
			}
			case TRIGGER_INVASION: {
				InvasionDefinition invasionDefinition = new ArrayList<>(invasionDefinitionDictionary.getAll()).get(gameContext.getRandom().nextInt(invasionDefinitionDictionary.getAll().size()));
				messageDispatcher.dispatchMessage(MessageType.TRIGGER_INVASION, invasionDefinition);
				break;
			}
			case TRIGGER_TRADE_CARAVAN: {
				messageDispatcher.dispatchMessage(MessageType.TRIGGER_TRADE_CARAVAN);
				break;
			}
			case NONE:
			default:
				// Do nothing
		}
	}

	@Override
	public void populate(Table containerTable) {
		update();
		containerTable.add(this.layoutTable);
	}

	@Override
	public void update() {
		if (gameContext != null) {

			layoutTable.clearChildren();
			if (displayed) {
				layoutTable.background("save_bg_patch");
				layoutTable.add(titleLabel).pad(15).row();
				layoutTable.add(actionSelect).pad(15).left().row();

				if (currentAction.equals(DebugAction.SPAWN_ITEM)) {
					layoutTable.add(itemTypeSelect).pad(15).left().row();
					layoutTable.add(materialSelect).pad(15).left().row();
				} else if (currentAction.equals(DebugAction.TOGGLE_PIPE)) {
					layoutTable.add(materialSelect).pad(15).left().row();
				} else if (currentAction.equals(DebugAction.SPAWN_CREATURE)) {
					layoutTable.add(raceSelect).pad(15).left().row();
				} else if (currentAction.equals(DebugAction.CHANGE_CREATURE_NEED)) {
					layoutTable.add(needSelect).pad(15).left().row();
					layoutTable.add(needValueSelect).pad(15).left().row();
				} else if (currentAction.equals(DebugAction.DESTROY_BODY_PART)) {


					layoutTable.add(bodyPartSelect).pad(15).left().row();
				} else if (currentAction.equals(DebugAction.SPAWN_PLANT)) {
					layoutTable.add(plantSpeciesSelect).pad(15).left().row();
					layoutTable.add(plantSpeciesGrowthStageSelect).pad(15).left().row();
				} else if (currentAction.equals(DebugAction.SPAWN_VEHICLE)) {
					layoutTable.add(vehicleTypeSelect).pad(15).left().row();
				}
			} else {
				layoutTable.setBackground((Drawable) null);
			}
		}
	}

	@Override
	public GuiViewName getName() {
		// This is a special case GuiView which lives outside of the normal usage
		return GuiViewName.DEBUG;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}

	private void resetMaterialSelect() {
		if (currentAction.equals(DebugAction.SPAWN_ITEM)) {
			resetMaterialSelectionForType(itemTypeToSpawn.getPrimaryMaterialType());
		} else if (currentAction.equals(DebugAction.TOGGLE_PIPE)) {
			resetMaterialSelectionForType(GameMaterialType.STONE);
		}
	}

	private void resetMaterialSelectionForType(GameMaterialType materialType) {
		List<GameMaterial> eligibleMaterials = materialDictionary.getByType(materialType);

		Array<GameMaterial> selectOptions = new Array<>();
		eligibleMaterials.forEach(m -> selectOptions.add(m));
		selectedMaterial = selectOptions.get(0);
		materialSelect.setSelected(selectOptions.get(0));
		materialSelect.setItems(selectOptions);
	}
}
