package technology.rocketjump.saul.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.AssetDisposable;
import technology.rocketjump.saul.combat.CombatTracker;
import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.factories.PlantEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.PlantEntityFactory;
import technology.rocketjump.saul.entities.factories.SettlerCreatureAttributesFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.settlement.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static technology.rocketjump.saul.entities.components.Faction.SETTLEMENT;
import static technology.rocketjump.saul.entities.model.EntityType.ITEM;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

@Singleton
public class EntityStore implements GameContextAware, AssetDisposable {

	private final SettlerCreatureAttributesFactory settlerCreatureAttributesFactory;

	private final PlantEntityAttributesFactory plantEntityAttributesFactory;
	private final PlantEntityFactory plantEntityFactory;

	private final ItemTypeDictionary itemTypeDictionary;
	private final ItemEntityFactory itemEntityFactory;

	private final Map<Long, Entity> updateEveryFrameEntities = new ConcurrentHashMap<>();
	private final List<Entity> updateInfrequentlyEntities = new ArrayList<>();
	private final Map<Long, Entity> jobAssignableEntities = new ConcurrentHashMap<>();

	private GameContext gameContext;
	private final SettlementFurnitureTracker settlementFurnitureTracker;
	private final SettlementItemTracker settlementItemTracker;
	private final SettlerTracker settlerTracker;
	private final CreatureTracker creatureTracker;
	private final VehicleTracker vehicleTracker;
	private final CombatTracker combatTracker;
	private final ConstantsRepo constantsRepo;

	@Inject
	public EntityStore(SettlerCreatureAttributesFactory settlerCreatureAttributesFactory,
					   PlantEntityAttributesFactory plantEntityAttributesFactory, PlantEntityFactory plantEntityFactory,
					   ItemTypeDictionary itemTypeDictionary, ItemEntityFactory itemEntityFactory,
					   SettlementFurnitureTracker settlementFurnitureTracker,
					   SettlementItemTracker settlementItemTracker, SettlerTracker settlerTracker, CreatureTracker creatureTracker,
					   VehicleTracker vehicleTracker, CombatTracker combatTracker, ConstantsRepo constantsRepo) {
		this.settlerCreatureAttributesFactory = settlerCreatureAttributesFactory;
		this.plantEntityAttributesFactory = plantEntityAttributesFactory;
		this.plantEntityFactory = plantEntityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.settlementFurnitureTracker = settlementFurnitureTracker;
		this.settlementItemTracker = settlementItemTracker;
		this.settlerTracker = settlerTracker;
		this.creatureTracker = creatureTracker;
		this.vehicleTracker = vehicleTracker;
		this.combatTracker = combatTracker;
		this.constantsRepo = constantsRepo;
	}

	public void add(Entity entity) {
		add(entity, true);
	}

	private void add(Entity entity, boolean addToMap) {
		gameContext.getEntities().put(entity.getId(), entity);
		if (entity.isUpdateEveryFrame()) {
			updateEveryFrameEntities.put(entity.getId(), entity);
		}
		if (entity.isUpdateInfrequently()) {
			updateInfrequentlyEntities.add(entity);
		}
		if (entity.isJobAssignable()) {
			jobAssignableEntities.put(entity.getId(), entity);
		}
		if (addToMap && entity.getLocationComponent().getWorldPosition() != null) {
			GridPoint2 entityTilePosition = toGridPoint(entity.getLocationComponent().getWorldPosition());
			gameContext.getAreaMap().getTile(entityTilePosition).addEntity(entity);

			for (GridPoint2 extraTilePosition : entity.calculateOtherTilePositions()) {
				MapTile otherTile = gameContext.getAreaMap().getTile(extraTilePosition);
				if (otherTile != null) {
					otherTile.addEntity(entity);
				}
			}
		}
	}

	public Entity remove(long entityId) {
		return remove(entityId, false);
	}

	public Entity remove(long entityId, boolean removeFromWorld) {
		Entity entityToRemove = gameContext.getEntities().remove(entityId);
		if (entityToRemove != null) {
			if (entityToRemove.isUpdateEveryFrame()) {
				updateEveryFrameEntities.remove(entityId);
			}
			if (entityToRemove.isUpdateInfrequently()) {
				updateInfrequentlyEntities.remove(entityToRemove);
			}

			if (removeFromWorld && entityToRemove.getLocationComponent().getWorldPosition() != null) {
				GridPoint2 entityTilePosition = toGridPoint(entityToRemove.getLocationComponent().getWorldPosition());
				gameContext.getAreaMap().getTile(entityTilePosition).removeEntity(entityId);

				for (GridPoint2 extraTilePosition : entityToRemove.calculateOtherTilePositions()) {
					MapTile otherTile = gameContext.getAreaMap().getTile(extraTilePosition);
					if (otherTile != null) {
						otherTile.removeEntity(entityId);
					}
				}
			}
		}
		return entityToRemove;
	}

	public Entity createPlantForMap(String speciesName, GridPoint2 worldPosition, Random random) {
		PlantEntityAttributes attributes = plantEntityAttributesFactory.createBySpeciesName(speciesName);
		if (attributes == null) {
			Logger.error("Could not find plant species with name " + speciesName);
			return null;
		}
		int initialGrowthStage = 0;
		if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.TREE)) {
			initialGrowthStage = random.nextBoolean() ? 2 : 3;
		} else if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.MUSHROOM_TREE)) {
			initialGrowthStage = attributes.getSpecies().getGrowthStages().size() - 1;
		}
		attributes.setGrowthStageCursor(initialGrowthStage);

		attributes.setGrowthStageProgress(0.4f + (random.nextFloat() * 0.5f));
		Entity entity = plantEntityFactory.create(attributes, worldPosition, gameContext);
		add(entity);
		return entity;
	}

	public void changeBehaviour(Entity entity, BehaviourComponent newBehaviour, MessageDispatcher messageDispatcher) {
		if (entity != null) {
			BehaviourComponent oldBehaviour = entity.getBehaviourComponent();
			if (oldBehaviour != null) {
				if (oldBehaviour.isJobAssignable()) {
					jobAssignableEntities.remove(entity.getId());
				}
				if (oldBehaviour.isUpdateEveryFrame()) {
					updateEveryFrameEntities.remove(entity.getId());
				}
				if (oldBehaviour.isUpdateInfrequently()) {
					updateInfrequentlyEntities.remove(entity);
				}
				if (oldBehaviour instanceof Destructible) {
					((Destructible)oldBehaviour).destroy(entity, messageDispatcher, gameContext);
				}
			}

			entity.replaceBehaviourComponent(newBehaviour);
			newBehaviour.init(entity, messageDispatcher, gameContext);

			if (newBehaviour.isJobAssignable()) {
				jobAssignableEntities.put(entity.getId(), entity);
			}
			if (newBehaviour.isUpdateEveryFrame()) {
				updateEveryFrameEntities.put(entity.getId(), entity);
			}
			if (newBehaviour.isUpdateInfrequently()) {
				updateInfrequentlyEntities.add(entity);
			}
		}
	}

	public void createResourceItem(GameMaterial mainMaterial, GridPoint2 worldPosition,
								   int quantity, GameMaterial... otherMaterials) {
		ItemEntityAttributes itemEntityAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());

		String itemTypeName;
		switch (mainMaterial.getMaterialType()) {
			case STONE:
				itemTypeName = constantsRepo.getWorldConstants().getStoneHarvestedItemType();
				break;
			case ORE:
				itemTypeName = constantsRepo.getWorldConstants().getOreHarvestedItemType();
				break;
			case GEM:
				itemTypeName = constantsRepo.getWorldConstants().getGemHarvestedItemType();
				break;
			default:
				Logger.error("Not yet implemented resource item creation from " + mainMaterial.getMaterialType());
				return;
		}
		itemEntityAttributes.setItemType(itemTypeDictionary.getByName(itemTypeName));

		itemEntityAttributes.setMaterial(mainMaterial);
		for (GameMaterial otherMaterial : otherMaterials) {
			itemEntityAttributes.setMaterial(otherMaterial);
		}

		itemEntityAttributes.setQuantity(quantity);
		createResourceItem(itemEntityAttributes, worldPosition);
	}

	public void createResourceItem(ItemEntityAttributes itemEntityAttributes, GridPoint2 worldPosition) {
		MapTile tile = gameContext.getAreaMap().getTile(worldPosition);
		if (tile != null) {
			// Check to see if we should merge into an existing item
			Entity matchingItem = tile.getItemMatching(itemEntityAttributes);
			if (matchingItem == null) {
				itemEntityFactory.create(itemEntityAttributes, worldPosition, true, this.gameContext, Faction.SETTLEMENT);
			} else {
				ItemEntityAttributes attributes = (ItemEntityAttributes) matchingItem.getPhysicalEntityComponent().getAttributes();
				attributes.setQuantity(attributes.getQuantity() + itemEntityAttributes.getQuantity());
			}
		}
	}

	public Iterable<Entity> getJobAssignableEntities() {
		return jobAssignableEntities.values();
	}

	public void shuffle() {
		Collections.shuffle(updateInfrequentlyEntities);
	}

	public Entity getById(long entityId) {
		return gameContext.getEntities().get(entityId);
	}

	public Iterable<Entity> getAllEntities() {
		return gameContext.getEntities().values();
	}

	public Iterable<Entity> getUpdateEveryFrameEntities() {
		return this.updateEveryFrameEntities.values();
	}

	public List<Entity> getUpdateInfrequentlyEntities() {
		return updateInfrequentlyEntities;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;

		if (gameContext != null) {
			for (Entity entity : gameContext.getEntities().values()) {
				add(entity, false);
				Faction faction = entity.getOrCreateComponent(FactionComponent.class).getFaction();

				switch (entity.getType()) {
					case ITEM:
						if (faction.equals(SETTLEMENT)) {
							settlementItemTracker.itemAdded(entity);
						}
						break;
					case FURNITURE:
						if (faction.equals(SETTLEMENT)) {
							settlementFurnitureTracker.furnitureAdded(entity);
						}
						break;
					case CREATURE:
						if (entity.getBehaviourComponent() instanceof CorpseBehaviour) {
							if (entity.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
								settlerTracker.settlerDied(entity);
							} else {
								creatureTracker.creatureDied(entity);
							}
						} else {
							if (entity.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
								settlerTracker.settlerAdded(entity);
								InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
								if (inventoryComponent != null) {
									for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
										add(inventoryEntry.entity);
										if (inventoryEntry.entity.getType().equals(ITEM)) {
											settlementItemTracker.itemAdded(inventoryEntry.entity);
										}
									}
								}
								HaulingComponent haulingComponent = entity.getComponent(HaulingComponent.class);
								if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
									add(haulingComponent.getHauledEntity());
									if (haulingComponent.getHauledEntity().getType().equals(ITEM)) {
										settlementItemTracker.itemAdded(haulingComponent.getHauledEntity());
									}
								}
							} else {
								creatureTracker.creatureAdded(entity);
							}
						}
						break;
					case VEHICLE:
						vehicleTracker.vehicleAdded(entity);
						break;
				}
			}
		}
	}

	@Override
	public void clearContextRelatedState() {
		updateEveryFrameEntities.clear();
		updateInfrequentlyEntities.clear();
		jobAssignableEntities.clear();
	}

	@Override
	public void dispose() {
		// Need to dispose all entities to free up assets like point light meshes
		if (gameContext != null) {
			for (Entity entity : getAllEntities()) {
				entity.dispose();
			}
		}

	}
}
