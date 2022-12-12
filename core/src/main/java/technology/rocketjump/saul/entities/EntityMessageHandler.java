package technology.rocketjump.saul.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.BrokenDwarfBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.InvasionCreatureGroup;
import technology.rocketjump.saul.entities.behaviour.furniture.*;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.*;
import technology.rocketjump.saul.entities.components.furniture.*;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.entities.model.physical.creature.status.Death;
import technology.rocketjump.saul.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesItem;
import technology.rocketjump.saul.entities.tags.TransformToItemsOnDeathTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.gamecontext.GameState;
import technology.rocketjump.saul.jobs.JobFactory;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobState;
import technology.rocketjump.saul.jobs.model.JobTarget;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.designation.Designation;
import technology.rocketjump.saul.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.rooms.components.StockpileRoomComponent;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.settlement.*;
import technology.rocketjump.saul.settlement.notifications.Notification;
import technology.rocketjump.saul.settlement.notifications.NotificationType;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.*;

import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.METAL_COLOR;
import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.SKIN_COLOR;
import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.saul.entities.ai.goap.actions.CancelLiquidAllocationAction.cancelLiquidAllocation;
import static technology.rocketjump.saul.entities.ai.goap.actions.SleepOnFloorAction.changeToConsciousnessOnFloor;
import static technology.rocketjump.saul.entities.ai.goap.actions.SleepOnFloorAction.showAsRotatedOnSide;
import static technology.rocketjump.saul.entities.components.Faction.SETTLEMENT;
import static technology.rocketjump.saul.entities.components.ItemAllocation.AllocationState.CANCELLED;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.HAULING;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.saul.entities.model.EntityType.*;
import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.AWAKE;
import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.DEAD;
import static technology.rocketjump.saul.entities.model.physical.furniture.EntityDestructionCause.OXIDISED;
import static technology.rocketjump.saul.jobs.JobMessageHandler.deconstructFurniture;
import static technology.rocketjump.saul.messaging.MessageType.*;
import static technology.rocketjump.saul.military.model.SquadOrderType.COMBAT;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;
import static technology.rocketjump.saul.rooms.HaulingAllocation.AllocationPositionType.CREATURE;

@Singleton
public class EntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final JobFactory jobFactory;
	private final EntityStore entityStore;
	private final SettlementItemTracker settlementItemTracker;
	private final SettlementFurnitureTracker settlementFurnitureTracker;
	private final SettlerTracker settlerTracker;
	private final CreatureTracker creatureTracker;
	private final OngoingEffectTracker ongoingEffectTracker;
	private final RoomStore roomStore;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final ItemEntityFactory itemEntityFactory;
	private final ItemTypeDictionary itemTypeDictionary;
	private final I18nTranslator i18nTranslator;
	private final JobStore jobStore;
	private final GameMaterialDictionary materialDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final SoundAsset treeFallSoundEffect;
	private GameContext gameContext;
	private ParticleEffectType leafExplosionParticleType;
	private ParticleEffectType chipExplosionParticleType;
	private ParticleEffectType treeShedLeafEffect;
	private ParticleEffectType liquidSplashEffect;
	private ParticleEffectType deconstructParticleEffect;
	private Designation deconstructDesignation;

	@Inject
	public EntityMessageHandler(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater,
								JobFactory jobFactory, EntityStore entityStore, SettlementItemTracker settlementItemTracker,
								SettlementFurnitureTracker settlementFurnitureTracker, SettlerTracker settlerTracker, CreatureTracker creatureTracker,
								OngoingEffectTracker ongoingEffectTracker, RoomStore roomStore,
								ItemEntityAttributesFactory itemEntityAttributesFactory, ItemEntityFactory itemEntityFactory,
								ItemTypeDictionary itemTypeDictionary, I18nTranslator i18nTranslator, JobStore jobStore,
								GameMaterialDictionary materialDictionary, SoundAssetDictionary soundAssetDictionary,
								ParticleEffectTypeDictionary particleEffectTypeDictionary, DesignationDictionary designationDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
		this.jobFactory = jobFactory;
		this.entityStore = entityStore;
		this.settlementItemTracker = settlementItemTracker;
		this.settlementFurnitureTracker = settlementFurnitureTracker;
		this.settlerTracker = settlerTracker;
		this.creatureTracker = creatureTracker;
		this.ongoingEffectTracker = ongoingEffectTracker;
		this.roomStore = roomStore;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.itemEntityFactory = itemEntityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.i18nTranslator = i18nTranslator;
		this.jobStore = jobStore;
		this.materialDictionary = materialDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.deconstructDesignation = designationDictionary.getByName("DECONSTRUCT");


		this.leafExplosionParticleType = particleEffectTypeDictionary.getByName("Leaf explosion"); // MODDING expose this
		this.chipExplosionParticleType = particleEffectTypeDictionary.getByName("Chip explosion"); // MODDING expose this
		treeShedLeafEffect = particleEffectTypeDictionary.getByName("Falling leaf");
		liquidSplashEffect = particleEffectTypeDictionary.getByName("Liquid splash");
		deconstructParticleEffect = particleEffectTypeDictionary.getByName("Dust cloud above");
		this.treeFallSoundEffect = this.soundAssetDictionary.getByName("Mining Drop");

		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY);
		messageDispatcher.addListener(this, MessageType.JOB_REMOVED);
		messageDispatcher.addListener(this, MessageType.JOB_CANCELLED);
		messageDispatcher.addListener(this, MessageType.TREE_FELLED);
		messageDispatcher.addListener(this, MessageType.ENTITY_ASSET_UPDATE_REQUIRED);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
		messageDispatcher.addListener(this, MessageType.ENTITY_DO_NOT_TRACK);
		messageDispatcher.addListener(this, MessageType.ITEM_PRIMARY_MATERIAL_CHANGED);
		messageDispatcher.addListener(this, MessageType.REQUEST_DOOR_OPEN);
		messageDispatcher.addListener(this, MessageType.REQUEST_FURNITURE_REMOVAL);
		messageDispatcher.addListener(this, MessageType.HAULING_ALLOCATION_CANCELLED);
		messageDispatcher.addListener(this, MessageType.CHANGE_PROFESSION);
		messageDispatcher.addListener(this, MessageType.APPLY_STATUS);
		messageDispatcher.addListener(this, MessageType.REMOVE_STATUS);
		messageDispatcher.addListener(this, MessageType.TRANSFORM_FURNITURE_TYPE);
		messageDispatcher.addListener(this, MessageType.TRANSFORM_ITEM_TYPE);
		messageDispatcher.addListener(this, MessageType.CREATURE_DEATH);
		messageDispatcher.addListener(this, MessageType.SAPIENT_CREATURE_INSANITY);
		messageDispatcher.addListener(this, MessageType.SETTLER_TANTRUM);
		messageDispatcher.addListener(this, MessageType.LIQUID_SPLASH);
		messageDispatcher.addListener(this, MessageType.TREE_SHED_LEAVES);
		messageDispatcher.addListener(this, MessageType.FURNITURE_IN_USE);
		messageDispatcher.addListener(this, MessageType.FURNITURE_NO_LONGER_IN_USE);
		messageDispatcher.addListener(this, MessageType.DAMAGE_FURNITURE);
		messageDispatcher.addListener(this, MessageType.MATERIAL_OXIDISED);
		messageDispatcher.addListener(this, MessageType.FIND_BUTCHERABLE_UNALLOCATED_CORPSE);
		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY_AND_ALL_INVENTORY);
		messageDispatcher.addListener(this, MessageType.ENTITY_FACTION_CHANGED);
		messageDispatcher.addListener(this, MessageType.CHANGE_ENTITY_BEHAVIOUR);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ENTITY_CREATED: {
				Entity createdEntity = (Entity) msg.extraInfo;
				entityStore.add(createdEntity);
				Faction faction = createdEntity.getOrCreateComponent(FactionComponent.class).getFaction();

				if (createdEntity.getBehaviourComponent() instanceof CreatureBehaviour) {
					if (createdEntity.isSettler()) {
						settlerTracker.settlerAdded(createdEntity);
					} else {
						creatureTracker.creatureAdded(createdEntity);
					}
					InventoryComponent inventoryComponent = createdEntity.getComponent(InventoryComponent.class);
					if (inventoryComponent != null) {
						for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
							if (inventoryEntry.entity.getType().equals(ITEM) && faction.equals(SETTLEMENT)) {
								settlementItemTracker.itemAdded(inventoryEntry.entity);
							}
						}
					}
				} else if (createdEntity.getType().equals(ITEM) && faction.equals(SETTLEMENT)) {
					settlementItemTracker.itemAdded(createdEntity);
				} else if (createdEntity.getType().equals(EntityType.FURNITURE) && faction.equals(SETTLEMENT)) {
					settlementFurnitureTracker.furnitureAdded(createdEntity);
				} else if (createdEntity.getType().equals(ONGOING_EFFECT)) {
					ongoingEffectTracker.entityAdded(createdEntity);
				}

				return true;
			}
			case ENTITY_FACTION_CHANGED: {
				FactionChangedMessage message = (FactionChangedMessage) msg.extraInfo;

				if (message.entity().getType().equals(ITEM)) {
					if (message.newFaction().equals(SETTLEMENT)) {
						settlementItemTracker.itemAdded(message.entity());
					} else {
						settlementItemTracker.itemRemoved(message.entity());
					}
				} else if (message.entity().getType().equals(FURNITURE)) {
					if (message.newFaction().equals(SETTLEMENT)) {
						settlementFurnitureTracker.furnitureAdded(message.entity());
					} else {
						settlementFurnitureTracker.furnitureRemoved(message.entity());
					}
				}
				return true;
			}
			case MessageType.DESTROY_ENTITY_AND_ALL_INVENTORY: {
				Entity entity = (Entity) msg.extraInfo;
				if (entity == null) {
					return true;
				}

				InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
				if (inventoryComponent != null) {
					inventoryComponent.destroyAllEntities(messageDispatcher);
				}
				EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
				if (equippedItemComponent != null) {
					equippedItemComponent.destroyAllEntities(messageDispatcher);
				}
				// Fall-through to DESTROY_ENTITY
			}
			case MessageType.DESTROY_ENTITY: {
				Entity entity = (Entity) msg.extraInfo;
				if (entity == null) {
					return true;
				}
				Entity removedEntity = entityStore.getById(entity.getId());
				if (removedEntity != null) {
					removedEntity.destroy(messageDispatcher, gameContext);
					// Need to remove after destroy() so things can clean their state up while the entity still exists
					entityStore.remove(entity.getId());
					if (removedEntity.getType().equals(ITEM)) {
						settlementItemTracker.itemRemoved(removedEntity);
					} else if (removedEntity.getType().equals(EntityType.FURNITURE)) {
						settlementFurnitureTracker.furnitureRemoved(removedEntity);
					} else if (removedEntity.getType().equals(ONGOING_EFFECT)) {
						ongoingEffectTracker.entityRemoved(removedEntity);
					} else if (removedEntity.getBehaviourComponent() instanceof CreatureBehaviour) {
						if (removedEntity.isSettler()) {
							settlerTracker.settlerRemoved(removedEntity);
							CreatureEntityAttributes entityAttributes = (CreatureEntityAttributes) removedEntity.getPhysicalEntityComponent().getAttributes();
							if (!entityAttributes.getConsciousness().equals(DEAD)) {
								// Destroying non-dead settler entity
								handle(new CreatureDeathMessage(removedEntity, DeathReason.UNKNOWN, null));
							}
						} else {
							creatureTracker.creatureRemoved(removedEntity);
						}
						removeFromSquadOrders(removedEntity);
					}

					if (removedEntity.getLocationComponent().getWorldPosition() != null) {
						List<MapTile> allTiles = new ArrayList<>();
						MapTile mapTile = gameContext.getAreaMap().getTile(removedEntity.getLocationComponent().getWorldPosition());
						if (mapTile != null) {
							mapTile.removeEntity(removedEntity.getId());
						}

						// TODO Maybe this should be refactored into a MultiTileEntityComponent which defines how an entity bridges extra tiles
						if (removedEntity.getType().equals(EntityType.FURNITURE)) {
							FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) removedEntity.getPhysicalEntityComponent().getAttributes();
							for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
								MapTile extraTile = gameContext.getAreaMap().getTile(mapTile.getTilePosition().cpy().add(extraTileOffset));
								if (extraTile != null) {
									extraTile.removeEntity(removedEntity.getId());
								}
							}
						} else if (removedEntity.getType().equals(PLANT)) {
							PlantEntityAttributes attributes = (PlantEntityAttributes) removedEntity.getPhysicalEntityComponent().getAttributes();
							if (attributes.getRemovePestsJob() != null && !attributes.getRemovePestsJob().getJobState().equals(JobState.REMOVED)) {
								messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, attributes.getRemovePestsJob());
							}
						}
					}
					Entity containerEntity = removedEntity.getLocationComponent().getContainerEntity();
					if (containerEntity != null) {
						InventoryComponent containerInventory = containerEntity.getComponent(InventoryComponent.class);
						EquippedItemComponent equippedItemComponent = containerEntity.getComponent(EquippedItemComponent.class);
						HaulingComponent haulingComponent = containerEntity.getComponent(HaulingComponent.class);
						AttachedEntitiesComponent attachedEntitiesComponent = containerEntity.getComponent(AttachedEntitiesComponent.class);

						if (haulingComponent != null && haulingComponent.getHauledEntity() != null && haulingComponent.getHauledEntity().getId() == removedEntity.getId()) {
							haulingComponent.clearHauledEntity();
							containerEntity.removeComponent(HaulingComponent.class);
						}
						if (equippedItemComponent != null) {
							if (equippedItemComponent.getMainHandItem() != null && equippedItemComponent.getMainHandItem().getId() == removedEntity.getId()) {
								equippedItemComponent.clearMainHandItem();
							}
							if (equippedItemComponent.getOffHandItem() != null && equippedItemComponent.getOffHandItem().getId() == removedEntity.getId()) {
								equippedItemComponent.clearOffHandItem();
							}
							if (equippedItemComponent.getEquippedClothing() != null && equippedItemComponent.getEquippedClothing().getId() == removedEntity.getId()) {
								equippedItemComponent.clearEquippedClothing();
							}
						}
						if (containerInventory != null) {
							containerInventory.remove(removedEntity.getId());
						}
						if (attachedEntitiesComponent != null) {
							attachedEntitiesComponent.remove(removedEntity);
							if (attachedEntitiesComponent.getAttachedEntities().isEmpty()) {
								containerEntity.removeComponent(AttachedEntitiesComponent.class);
							}
						}
					}
				} else {
					// Might be an untracked entity like a pipe
					MapTile parentTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldOrParentPosition());
					if (parentTile != null && parentTile.hasPipe() && parentTile.getUnderTile().getPipeEntity().equals(entity)) {
						parentTile.getUnderTile().setPipeEntity(null);
					}
				}
				return true;
			}
			case MessageType.ENTITY_DO_NOT_TRACK: {
				Entity entity = (Entity) msg.extraInfo;
				entity.getLocationComponent().setUntracked(true);

				if (entity.getType().equals(ITEM)) {
					settlementItemTracker.itemRemoved(entity);
				} else if (entity.getType().equals(EntityType.FURNITURE)) {
					settlementFurnitureTracker.furnitureRemoved(entity);
				} else if (entity.getType().equals(CREATURE)) {
					if (entity.isSettler()) {
						settlerTracker.settlerRemoved(entity);
					} else {
						creatureTracker.creatureRemoved(entity);
					}
				}

				return true;
			}
			case MessageType.ITEM_PRIMARY_MATERIAL_CHANGED: {
				ItemPrimaryMaterialChangedMessage message = (ItemPrimaryMaterialChangedMessage) msg.extraInfo;
				settlementItemTracker.primaryMaterialChanged(message.item, message.oldPrimaryMaterial);
				return true;
			}
			case MessageType.JOB_REMOVED: {
				Job removedJob = (Job) msg.extraInfo;
				Long entityId = removedJob.getAssignedToEntityId();
				if (entityId != null) {
					Entity entity = entityStore.getById(entityId);
					if (entity != null && entity.getBehaviourComponent() instanceof CreatureBehaviour behaviour) {
						if (behaviour.getCurrentGoal() != null) {
							behaviour.getCurrentGoal().setInterrupted(true);
						}
					}
				}

				jobStore.remove(removedJob);
				// Fall through to job cancelled behaviour
			}
			case MessageType.JOB_CANCELLED: {
				Job removedJob = (Job) msg.extraInfo;

				if (removedJob.getHaulingAllocation() != null) {
					messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, removedJob.getHaulingAllocation());
					removedJob.setHaulingAllocation(null);
				}

				if (removedJob.getLiquidAllocation() != null) {
					cancelLiquidAllocation(removedJob.getLiquidAllocation(), gameContext);
					removedJob.setLiquidAllocation(null);
				}


				if (removedJob.getType().getName().equals("DECONSTRUCT")) {
					Long potentialTargetEntityId = removedJob.getTargetId();
					if (potentialTargetEntityId != null) {
						Entity entity = entityStore.getById(potentialTargetEntityId);
						if (entity != null) {
							ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
							if (constructedEntityComponent != null) {
								constructedEntityComponent.setDeconstructionJob(null);
							}
						}
					}
				}
				return true;
			}
			case MessageType.TREE_FELLED: {
				return handle((TreeFallenMessage) msg.extraInfo);
			}
			case MessageType.TREE_SHED_LEAVES: {
				ShedLeavesMessage message = (ShedLeavesMessage) msg.extraInfo;
				if (message.leafColor != null && !message.leafColor.equals(Color.CLEAR)) {
					messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(treeShedLeafEffect,
							Optional.of(message.parentEntity), Optional.of(new JobTarget(message.parentEntity)), (p) -> {
						p.getWrappedInstance().setTint(message.leafColor);
					}));
				}
				return true;
			}
			case MessageType.ENTITY_ASSET_UPDATE_REQUIRED: {
				Entity entity = (Entity) msg.extraInfo;
				if (entity != null) {
					entityAssetUpdater.updateEntityAssets(entity);
				}
				return true;
			}
			case MessageType.REQUEST_DOOR_OPEN: {
				Entity doorEntity = (Entity) msg.extraInfo;
				DoorBehaviour doorBehaviour = (DoorBehaviour) doorEntity.getBehaviourComponent();
				doorBehaviour.doorOpenRequested();
				return true;
			}
			case MessageType.REQUEST_FURNITURE_REMOVAL: {
				Entity entity = (Entity) msg.extraInfo;
				ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
				MapTile entityTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldPosition());
				if (entityTile != null) {
					if (constructedEntityComponent.isAutoConstructed()) {
						// FIXME This and its shared usage would be better dealt with by a ACTUALLY_DO_THE_DECONSTRUCT type message
						deconstructFurniture(entity, entityTile, messageDispatcher, gameContext, itemTypeDictionary, itemEntityAttributesFactory, itemEntityFactory,
								deconstructParticleEffect);
					} else if (!constructedEntityComponent.isBeingDeconstructed()) {
						Job deconstructionJob = jobFactory.deconstructionJob(entityTile);
						if (deconstructionJob != null) {
							constructedEntityComponent.setDeconstructionJob(deconstructionJob);
							messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, deconstructionJob);

							// also apply designation to other tiles
							if (deconstructDesignation != null) {
								Set<MapTile> locations = new HashSet<>();
								locations.add(entityTile);
								for (GridPoint2 extraOffset : ((FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getCurrentLayout().getExtraTiles()) {
									locations.add(gameContext.getAreaMap().getTile(entityTile.getTilePosition().cpy().add(extraOffset)));
								}


								for (MapTile location : locations) {
									if (location.getDesignation() == null) {
										location.setDesignation(deconstructDesignation);
									}
								}
							}
						}
					}
				}
				return true;
			}
			case MessageType.CHANGE_PROFESSION:
				return handle((ChangeProfessionMessage) msg.extraInfo);
			case MessageType.HAULING_ALLOCATION_CANCELLED: {
				HaulingAllocation allocation = (HaulingAllocation) msg.extraInfo;

				if (allocation.getLiquidAllocation() != null) {
					messageDispatcher.dispatchMessage(MessageType.LIQUID_ALLOCATION_CANCELLED, allocation.getLiquidAllocation());
				}

				if (allocation.getHauledEntityType().equals(EntityType.CREATURE)) {
					// Probably assigned to a piece of furniture somewhere
					if (allocation.getTargetPositionType().equals(HaulingAllocation.AllocationPositionType.FURNITURE)) {
						Entity targetFurniture = entityStore.getById(allocation.getTargetId());
						if (targetFurniture == null) {
							Logger.error("Could not find target furniture of cancelled hauling allocation for type " + CREATURE);
						} else {
							((FurnitureEntityAttributes) targetFurniture.getPhysicalEntityComponent().getAttributes()).setAssignedToEntityId(null);
						}
					}
					return true;
				} else if (allocation.getHauledEntityType().equals(EntityType.ITEM)) {
					if (allocation.getItemAllocation() == null) {
						Logger.warn("Item Hauling allocation does not have an item allocation");
						return true;
					}

					Entity targetItemEntity = entityStore.getById(allocation.getHauledEntityId());
					if (targetItemEntity == null) {
						// Entity must have been destroyed already
						return true;
					}
					if (!allocation.getItemAllocation().getState().equals(CANCELLED) &&
							!allocation.getItemAllocation().getPurpose().equals(HAULING) &&
							!allocation.getItemAllocation().getPurpose().equals(HELD_IN_INVENTORY)) {
						ItemAllocationComponent itemAllocationComponent = targetItemEntity.getOrCreateComponent(ItemAllocationComponent.class);
						itemAllocationComponent.cancel(allocation.getItemAllocation());
					}

					if (allocation.getTargetPositionType() != null) {
						switch (allocation.getTargetPositionType()) {
							case ROOM -> {
								Room targetRoom = roomStore.getById(allocation.getTargetId());
								if (targetRoom != null && targetRoom.getComponent(StockpileRoomComponent.class) != null) {
									targetRoom.getComponent(StockpileRoomComponent.class).allocationCancelled(allocation, targetItemEntity);
								}
							}
							case CONSTRUCTION -> {
								Construction targetConstruction = gameContext.getConstructions().get(allocation.getTargetId());
								if (targetConstruction != null) {
									targetConstruction.allocationCancelled(allocation);
								}
							}
							case CREATURE -> {
								Entity targetCreature = entityStore.getById(allocation.getTargetId());
								if (targetCreature != null) {
									ItemAssignmentComponent itemAssignmentComponent = targetCreature.getComponent(ItemAssignmentComponent.class);
									if (itemAssignmentComponent != null) {
										itemAssignmentComponent.getHaulingAllocations().remove(allocation);
									}
								}
							}
							case FURNITURE -> {
								Entity targetFurnitureEntity = entityStore.getById(allocation.getTargetId());
								if (targetFurnitureEntity != null && targetFurnitureEntity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
									((CraftingStationBehaviour) targetFurnitureEntity.getBehaviourComponent()).allocationCancelled(allocation);
								} else if (targetFurnitureEntity != null && targetFurnitureEntity.getComponent(FurnitureStockpileComponent.class) != null) {
									targetFurnitureEntity.getComponent(FurnitureStockpileComponent.class).getStockpile().cancelAllocation(allocation);
								} else if (
										targetFurnitureEntity != null && targetFurnitureEntity.getBehaviourComponent() instanceof CollectItemFurnitureBehaviour ||
										targetFurnitureEntity != null && targetFurnitureEntity.getBehaviourComponent() instanceof InnoculationLogBehaviour
								) {
									// Do nothing, CollectItemFurnitureBehaviour will deal with cancelled allocations, eventually, might want to improve this
								} else {
									// FIXME perhaps this is fine and we can do nothing
									// Currently this could be a target of a cooking cauldron or baked bread, which KitchenBehaviour would deal with
									Logger.error("Unrecognised item allocation cancelled with target of furniture");
								}
							}
							case ZONE -> {} // Hauling to zone doesn't matter about cancelling
							default -> Logger.error("HAULING_ALLOCATION_CANCELLED message received with unrecognised targetPositionType");
						}
					}
					return true;
				} else {
					// FURNITURE-type hauling handled elsewhere e.g. KitchenManager
					return false;
				}
			}
			case MessageType.APPLY_STATUS: {
				StatusMessage message = (StatusMessage) msg.extraInfo;
				if (message.statusClass != null) {
					try {
						StatusEffect statusEffect = message.statusClass.getDeclaredConstructor().newInstance();
						if (statusEffect instanceof Death) {
							((Death) statusEffect).setDeathReason(message.deathReason);
						}
						message.entity.getComponent(StatusComponent.class).apply(statusEffect);
					} catch (ReflectiveOperationException e) {
						Logger.error("Could not instantiate " + message.statusClass.getSimpleName() + " with expected constructor");
					}
				}
				return true;
			}
			case MessageType.REMOVE_STATUS: {
				StatusMessage message = (StatusMessage) msg.extraInfo;
				message.entity.getComponent(StatusComponent.class).remove(message.statusClass);
				return true;
			}
			case MessageType.TRANSFORM_FURNITURE_TYPE: {
				return handle((TransformFurnitureMessage) msg.extraInfo);
			}
			case MessageType.TRANSFORM_ITEM_TYPE: {
				return handle((TransformItemMessage) msg.extraInfo);
			}
			case MessageType.CREATURE_DEATH: {
				return handle((CreatureDeathMessage) msg.extraInfo);
			}
			case MessageType.SAPIENT_CREATURE_INSANITY: {
				return handleInsanity((Entity) msg.extraInfo);
			}
			case MessageType.SETTLER_TANTRUM: {
				return handleSettlerTantrum((Entity) msg.extraInfo);
			}
			case MessageType.LIQUID_SPLASH: {
				return handleLiquidSplash((LiquidSplashMessage) msg.extraInfo);
			}
			case MessageType.FURNITURE_IN_USE: {
				Entity furnitureEntity = (Entity) msg.extraInfo;
				FurnitureParticleEffectsComponent particleEffectsComponent = furnitureEntity.getComponent(FurnitureParticleEffectsComponent.class);
				if (particleEffectsComponent != null) {
					Optional<JobTarget> targetItem = Optional.empty();
					InventoryComponent inventoryComponent = furnitureEntity.getComponent(InventoryComponent.class);
					if (inventoryComponent != null && !inventoryComponent.isEmpty()) {
						InventoryComponent.InventoryEntry entry = inventoryComponent.getInventoryEntries().stream().findFirst().get();
						targetItem = Optional.of(new JobTarget(entry.entity));
					}

					for (ParticleEffectType particleEffectType : particleEffectsComponent.getParticleEffectsWhenInUse()) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(particleEffectType,
								Optional.of(furnitureEntity),
								targetItem,
								particleEffectsComponent.getCurrentParticleInstances()::add));
					}
				}
				return true;
			}
			case MessageType.FURNITURE_NO_LONGER_IN_USE: {
				Entity furnitureEntity = (Entity) msg.extraInfo;
				FurnitureParticleEffectsComponent particleEffectsComponent = furnitureEntity.getComponent(FurnitureParticleEffectsComponent.class);
				if (particleEffectsComponent != null) {
					particleEffectsComponent.releaseParticles();
				}
				return true;
			}
			case MessageType.DAMAGE_FURNITURE: {
				return handleFurnitureDamaged((FurnitureDamagedMessage)msg.extraInfo);
			}
			case MessageType.MATERIAL_OXIDISED: {
				return handleMaterialOxidised((OxidisationMessage) msg.extraInfo);
			}
			case MessageType.FIND_BUTCHERABLE_UNALLOCATED_CORPSE: {
				handleFindButcherableCorpse((RequestCorpseMessage) msg.extraInfo);
				return true;
			}
			case CHANGE_ENTITY_BEHAVIOUR: {
				ChangeEntityBehaviourMessage message = (ChangeEntityBehaviourMessage) msg.extraInfo;
				entityStore.changeBehaviour(message.entity(), message.newBehaviour(), messageDispatcher);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handleSettlerTantrum(Entity tantrumEntity) {
		Notification tantrumNotification = new Notification(NotificationType.SETTLER_TANTRUM,
				tantrumEntity.getLocationComponent().getWorldOrParentPosition(), new Selectable(tantrumEntity, 0));
		tantrumNotification.addTextReplacement("character", i18nTranslator.getDescription(tantrumEntity));
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, tantrumNotification);
		return true;
	}

	private boolean handle(ChangeProfessionMessage changeProfessionMessage) {
		if (changeProfessionMessage.entity == null || changeProfessionMessage.entity.getComponent(SkillsComponent.class) == null) {
			throw new RuntimeException("No entity in " + changeProfessionMessage.getClass().getSimpleName() + " handled in " + this.getClass().getSimpleName());
		}

		SkillsComponent skillsComponent = changeProfessionMessage.entity.getComponent(SkillsComponent.class);
		if (changeProfessionMessage.professionToReplace != null && changeProfessionMessage.newProfession != null) {
			skillsComponent.replace(changeProfessionMessage.professionToReplace, changeProfessionMessage.newProfession);
		}

		// Remove any attached light sources so changing between mining helmet and not does not leave a rogue lightsource
		changeProfessionMessage.entity.removeComponent(AttachedLightSourceComponent.class);

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, changeProfessionMessage.entity);
		return true;
	}

	private boolean handle(TreeFallenMessage treeFallenMessage) {
		GridPoint2 treeTilePosition = new GridPoint2(
				(int) Math.floor(treeFallenMessage.getTreeWorldPosition().x),
				(int) Math.floor(treeFallenMessage.getTreeWorldPosition().y)
		);

		messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(treeFallSoundEffect, -1L,
				toVector(treeTilePosition), null));


		for (PlantSpeciesItem itemToCreate : treeFallenMessage.getItemsToCreate()) {
			int logsToCreateAtNextTile = 1;
			int logsLeftToCreate = itemToCreate.getQuantity();

			for (int xOffset = logsLeftToCreate; xOffset > 0; xOffset--) {
				GridPoint2 targetTilePosition;
				if (treeFallenMessage.isFallToWest()) {
					targetTilePosition = treeTilePosition.cpy().sub(xOffset, 0);
				} else {
					targetTilePosition = treeTilePosition.cpy().add(xOffset, 0);
				}

				MapTile targetTile = gameContext.getAreaMap().getTile(targetTilePosition);
				if (targetTile != null && targetTile.isNavigable(null)) {

					if (treeFallenMessage.getLeafColor().isPresent()) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(leafExplosionParticleType,
								Optional.empty(), Optional.of(new JobTarget(targetTile)), (p) -> {
							p.getWrappedInstance().setTint(treeFallenMessage.getLeafColor().get());
						}));
					}
					if (treeFallenMessage.getBranchColor() != null) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(chipExplosionParticleType,
								Optional.empty(), Optional.of(new JobTarget(targetTile)), (p) -> {
							p.getWrappedInstance().setTint(treeFallenMessage.getBranchColor());
						}));
					}

					ItemEntityAttributes itemEntityAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
					itemEntityAttributes.setColor(ColoringLayer.BRANCHES_COLOR, treeFallenMessage.getBranchColor());
					itemEntityAttributes.setQuantity(logsToCreateAtNextTile);
					itemEntityAttributes.setMaterial(itemToCreate.getMaterial());
					itemEntityAttributes.setItemType(itemToCreate.getItemType());


					Entity matchingItem = targetTile.getItemMatching(itemEntityAttributes);
					if (matchingItem == null && targetTile.hasItem()) {
						// There's a different kind of item here
					} else {
						// Else the item matches or the target doesn't have an item
						entityStore.createResourceItem(itemEntityAttributes, targetTilePosition);
						logsToCreateAtNextTile = 0;
					}
				}

				logsToCreateAtNextTile++;
			}

		}
		return true;
	}

	private boolean handle(TransformFurnitureMessage transformFurnitureMessage) {
		entityStore.remove(transformFurnitureMessage.furnitureEntity.getId(), true);
		settlementFurnitureTracker.furnitureRemoved(transformFurnitureMessage.furnitureEntity);

		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) transformFurnitureMessage.furnitureEntity.getPhysicalEntityComponent().getAttributes();
		FurnitureLayout originalLayout = attributes.getCurrentLayout();
		attributes.setFurnitureType(transformFurnitureMessage.transformToFurnitureType);
		if (!attributes.getCurrentLayout().equals(originalLayout)) {
			for (int i = 0; i < 4; i++) {
				attributes.setCurrentLayout(attributes.getCurrentLayout().getRotatesTo());
				if (attributes.getCurrentLayout().equals(originalLayout)) {
					break;
				}
			}
		}

		if (!transformFurnitureMessage.transformToFurnitureType.getRequirements().containsKey(attributes.getPrimaryMaterialType())) {
			for (GameMaterialType materialType : transformFurnitureMessage.transformToFurnitureType.getRequirements().keySet()) {
				if (attributes.getMaterials().get(materialType) != null) {
					attributes.setPrimaryMaterialType(materialType);
					break;
				}
			}
		}

		// Reset behaviour component
		BehaviourComponent oldBehaviour = transformFurnitureMessage.furnitureEntity.getBehaviourComponent();
		if (oldBehaviour instanceof Destructible) {
			((Destructible) oldBehaviour).destroy(transformFurnitureMessage.furnitureEntity, messageDispatcher, gameContext);
		}
		FurnitureBehaviour newBehaviour = new FurnitureBehaviour();
		newBehaviour.init(transformFurnitureMessage.furnitureEntity, messageDispatcher, gameContext);
		transformFurnitureMessage.furnitureEntity.replaceBehaviourComponent(newBehaviour);

		// Also re-applies any tags e.g. behaviour type
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, transformFurnitureMessage.furnitureEntity);

		if (oldBehaviour instanceof Prioritisable && transformFurnitureMessage.furnitureEntity.getBehaviourComponent() instanceof Prioritisable) {
			((Prioritisable) transformFurnitureMessage.furnitureEntity.getBehaviourComponent()).setPriority(((Prioritisable) oldBehaviour).getPriority());
		}

		entityStore.add(transformFurnitureMessage.furnitureEntity);
		if (transformFurnitureMessage.furnitureEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
			settlementFurnitureTracker.furnitureAdded(transformFurnitureMessage.furnitureEntity);
		}
		return true;
	}

	private boolean handle(TransformItemMessage transformItemMessage) {
		settlementItemTracker.itemRemoved(transformItemMessage.itemEntity);

		ItemEntityAttributes attributes = (ItemEntityAttributes) transformItemMessage.itemEntity.getPhysicalEntityComponent().getAttributes();
		attributes.setItemType(transformItemMessage.transformToItemType);

		// Also re-applies any tags
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, transformItemMessage.itemEntity);

		if (transformItemMessage.itemEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
			settlementItemTracker.itemAdded(transformItemMessage.itemEntity);
		}
		return true;
	}

	private boolean handle(CreatureDeathMessage deathMessage) {
		Entity deceased = deathMessage.deceased;
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) deceased.getPhysicalEntityComponent().getAttributes();
		Consciousness previousConciousness = attributes.getConsciousness();
		if (previousConciousness.equals(DEAD)) {
			// Already dead! Doesn't need killing again
			return true;
		}

		attributes.setConsciousness(DEAD);
		BehaviourComponent originalBehaviour = deceased.getBehaviourComponent();
		CorpseBehaviour corpseBehaviour = new CorpseBehaviour();
		corpseBehaviour.setOriginalSkinColor(attributes.getColor(SKIN_COLOR));
		entityStore.changeBehaviour(deceased, corpseBehaviour, messageDispatcher);

		Vector2 deceasedPosition = deceased.getLocationComponent().getWorldOrParentPosition();

		HistoryComponent historyComponent = deceased.getOrCreateComponent(HistoryComponent.class);
		DeathReason deathReason = deathMessage.reason;
		if (originalBehaviour instanceof BrokenDwarfBehaviour) {
			deathReason = DeathReason.GIVEN_UP_ON_LIFE;
		} else if (deathMessage.killer != null) {
			deathReason = DeathReason.KILLED_BY_ENTITY;
			historyComponent.setKilledBy(deathMessage.killer);
		}
		historyComponent.setDeathReason(deathReason);

		if (deceased.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
			Notification deathNotification = new Notification(NotificationType.DEATH, deceasedPosition, new Selectable(deceased, 0));
			deathNotification.addTextReplacement("character", i18nTranslator.getDescription(deceased));
			deathNotification.addTextReplacement("reason", i18nTranslator.getDictionary().getWord(deathReason.getI18nKey()));
			messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, deathNotification);

			settlerTracker.settlerDied(deceased);
		} else {
			creatureTracker.creatureDied(deceased);
		}

		dropEquippedItems(deceased, deceasedPosition);
		ItemAssignmentComponent itemAssignmentComponent = deceased.getComponent(ItemAssignmentComponent.class);
		if (itemAssignmentComponent != null) {
			itemAssignmentComponent.destroy(deceased, messageDispatcher, gameContext);
		}
		deceased.removeComponent(NeedsComponent.class);


		MilitaryComponent militaryComponent = deceased.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.isInMilitary()) {
			militaryComponent.removeFromMilitary();
		}

		// Rotate and change orientation of deceased
		if (previousConciousness.equals(AWAKE)) {
			changeToConsciousnessOnFloor(deceased, DEAD, gameContext, messageDispatcher);
		} else {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, deceased);
		}
		showAsRotatedOnSide(deceased, gameContext);

		removeFromSquadOrders(deceased);

		if (deceased.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
			boolean allDead = true;
			for (Entity settler : settlerTracker.getAll()) {
				CreatureEntityAttributes otherSettlerAttributes = (CreatureEntityAttributes) settler.getPhysicalEntityComponent().getAttributes();
				if (!otherSettlerAttributes.getConsciousness().equals(DEAD)) {
					allDead = false;
					break;
				}
			}

			if (allDead) {
				Notification gameOverNotification = new Notification(NotificationType.GAME_OVER, null, null);
				messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, gameOverNotification);
				gameContext.getSettlementState().setGameState(GameState.GAME_OVER);
			}
		}

		TransformToItemsOnDeathTag itemsOnDeathTag = deceased.getTag(TransformToItemsOnDeathTag.class);
		if (itemsOnDeathTag != null) {
			List<Entity> transformedEntities = itemsOnDeathTag.createItemEntities(messageDispatcher, itemTypeDictionary, attributes.getMaterials());
			transformedEntities.forEach(entity -> placeOnGround(entity, deceasedPosition));
			messageDispatcher.dispatchMessage(DESTROY_ENTITY, deceased);
		}

		if (deathMessage.killer != null && deathMessage.killer.getBehaviourComponent() instanceof CreatureBehaviour killerBehaviour &&
			killerBehaviour.getCreatureGroup() instanceof InvasionCreatureGroup invasionCreatureGroup) {
			invasionCreatureGroup.killedEnemy(deceased);
		}
		return true;
	}

	private void removeFromSquadOrders(Entity creatureEntity) {
		for (Squad squad : gameContext.getSquads().values()) {
			if (squad.getAttackEntityIds().contains(creatureEntity.getId())) {
				squad.getAttackEntityIds().remove(creatureEntity.getId());
				if (squad.getAttackEntityIds().isEmpty() && squad.getCurrentOrderType().equals(COMBAT)) {
					messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.TRAINING));
				}
			}
		}
	}

	private boolean handleInsanity(Entity entity) {
		BehaviourComponent currentBehaviour = entity.getBehaviourComponent();
		if (currentBehaviour instanceof Destructible) {
			((Destructible) currentBehaviour).destroy(entity, messageDispatcher, gameContext);
		}

		BrokenDwarfBehaviour brokenDwarfBehaviour = new BrokenDwarfBehaviour();
		entityStore.changeBehaviour(entity, brokenDwarfBehaviour, messageDispatcher);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);

		Vector2 entityPosition = entity.getLocationComponent().getWorldOrParentPosition();
		dropEquippedItems(entity, entityPosition);

		Notification brokenNotification = new Notification(NotificationType.SETTLER_MENTAL_BREAK, null, new Selectable(entity, 0));
		brokenNotification.addTextReplacement("character", i18nTranslator.getDescription(entity));
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, brokenNotification);

		return true;
	}

	private boolean handleLiquidSplash(LiquidSplashMessage message) {
		if (message.targetEntity != null && message.liquidMaterial != null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(liquidSplashEffect,
					Optional.of(message.targetEntity), Optional.empty(), (p) -> {
				p.getWrappedInstance().setTint(message.liquidMaterial.getColor());
			}));
		}
		return true;
	}

	private boolean handleFurnitureDamaged(FurnitureDamagedMessage message) {
		FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) message.targetEntity.getPhysicalEntityComponent().getAttributes();
		if (furnitureEntityAttributes.isDestroyed()) {
			return true;
		}
		furnitureEntityAttributes.setDestroyed(message.destructionCause);
		List<ColoringLayer> allColors = new ArrayList<>(furnitureEntityAttributes.getOtherColors().keySet());
		for (GameMaterial material : furnitureEntityAttributes.getMaterials().values()) {
			ColoringLayer coloringLayer = ColoringLayer.getByMaterialType(material.getMaterialType());
			if (coloringLayer != null) {
				allColors.add(coloringLayer);
			}
		}

		for (ColoringLayer coloringLayer : allColors) {
			if (METAL_COLOR.equals(coloringLayer)) {
				furnitureEntityAttributes.setColor(coloringLayer, message.metalColor);
			} else if (message.otherColor != null) {
				furnitureEntityAttributes.setColor(coloringLayer, message.otherColor);
			} else {
				Color color = furnitureEntityAttributes.getColor(coloringLayer);
				if (color != null) {
					Color newColor = color.cpy();
					newColor.r *= 0.3f;
					newColor.g *= 0.3f;
					newColor.b *= 0.3f;
					furnitureEntityAttributes.setColor(coloringLayer, newColor);
				}
			}
		}

		if (message.replacementPrimaryMaterial != null) {
			furnitureEntityAttributes.getMaterials().clear();
			furnitureEntityAttributes.getMaterials().put(furnitureEntityAttributes.getPrimaryMaterialType(), message.replacementPrimaryMaterial);
		}

		if (message.targetEntity.getBehaviourComponent() != null && !message.targetEntity.getBehaviourComponent().getClass().equals(FurnitureBehaviour.class)) {
			// remove crafting station or other behaviour
			entityStore.changeBehaviour(message.targetEntity, new FurnitureBehaviour(), messageDispatcher);
		}

		// Removes from usage such as beds
		settlementFurnitureTracker.furnitureRemoved(message.targetEntity);

		InventoryComponent inventoryComponent = message.targetEntity.getComponent(InventoryComponent.class);
		if (inventoryComponent != null) {
			for (InventoryComponent.InventoryEntry inventoryEntry : new ArrayList<>(inventoryComponent.getInventoryEntries())) {
				messageDispatcher.dispatchMessage(DESTROY_ENTITY, inventoryEntry.entity);
			}
		}
		DecorationInventoryComponent decorationInventoryComponent = message.targetEntity.getComponent(DecorationInventoryComponent.class);
		if (decorationInventoryComponent != null) {
			for (Entity decorationEntity : new ArrayList<>(decorationInventoryComponent.getDecorationEntities())) {
				messageDispatcher.dispatchMessage(DESTROY_ENTITY, decorationEntity);
			}
			decorationInventoryComponent.clear();
		}
		PoweredFurnitureComponent poweredFurnitureComponent = message.targetEntity.getComponent(PoweredFurnitureComponent.class);
		if (poweredFurnitureComponent != null) {
			poweredFurnitureComponent.destroy(message.targetEntity, messageDispatcher, gameContext);
		}

		LiquidContainerComponent liquidContainerComponent = message.targetEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent != null) {
			liquidContainerComponent.destroy(message.targetEntity, messageDispatcher, gameContext);
		}

		message.targetEntity.getLocationComponent().setRotation(slightRotation());

		return true;
	}

	private boolean handleMaterialOxidised(OxidisationMessage message) {
		EntityAttributes entityAttributes = message.targetEntity.getPhysicalEntityComponent().getAttributes();

		switch (message.oxidisedMaterial.getOxidisation().getEffect()) {

			case CONVERT_MATERIAL:
				// just swap out material

				GameMaterial newMaterial = materialDictionary.getByName(message.oxidisedMaterial.getOxidisation().getChangesTo());
				if (newMaterial == null) {
					Logger.error("Can not find material with name " + message.oxidisedMaterial.getOxidisation().getChangesTo() + " for oxidisation of " + message.oxidisedMaterial.getMaterialName());
				} else if (entityAttributes instanceof ItemEntityAttributes) {
					settlementItemTracker.itemRemoved(message.targetEntity);
					ItemEntityAttributes attributes = (ItemEntityAttributes) entityAttributes;
					attributes.removeMaterial(message.oxidisedMaterial.getMaterialType());
					attributes.setMaterial(newMaterial);
					if (message.oxidisedMaterial.getOxidisation().getSetsItemQualityTo() != null) {
						attributes.setItemQuality(message.oxidisedMaterial.getOxidisation().getSetsItemQualityTo());
					}
					if (message.targetEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
						settlementItemTracker.itemAdded(message.targetEntity);
					}
				} else if (entityAttributes instanceof FurnitureEntityAttributes) {
					settlementFurnitureTracker.furnitureRemoved(message.targetEntity);
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entityAttributes;
					attributes.removeMaterial(message.oxidisedMaterial.getMaterialType());
					attributes.setMaterial(newMaterial);
					if (message.targetEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(SETTLEMENT)) {
						settlementFurnitureTracker.furnitureAdded(message.targetEntity);
					}
				} else {
					Logger.error("Not yet implemented: material oxidised within " + entityAttributes.getClass().getSimpleName());
				}
				break;
			case DESTROY_PARENT:
				ItemType targetItemType = itemTypeDictionary.getByName(message.oxidisedMaterial.getOxidisation().getChangesTo());

				if (targetItemType == null) {
					Logger.error("Can not find item type with name " + message.oxidisedMaterial.getOxidisation().getChangesTo() + " for oxidisation of " + message.oxidisedMaterial.getMaterialName());
				} else if (entityAttributes instanceof ItemEntityAttributes) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entityAttributes;
					if (attributes.getPrimaryMaterial().equals(message.oxidisedMaterial)) {
						// Only destroy if parent primary material is that which oxidised

						if (attributes.getQuantity() == 0) {
							Logger.error("Should not be converting quantity 0 item");
						}

						messageDispatcher.dispatchMessage(TRANSFORM_ITEM_TYPE, new TransformItemMessage(message.targetEntity, targetItemType));
						attributes.getMaterials().clear();
						attributes.setMaterial(message.oxidisedMaterial);
						attributes.setDestroyed(OXIDISED);

						showNotificationOxidisationDestroyedSomething(message.targetEntity);

						// If this is within DecorationInventoryComponent, set furniture as destroyed
						if (message.targetEntity.getLocationComponent().getContainerEntity() != null) {
							DecorationInventoryComponent decorationInventoryComponent = message.targetEntity.getLocationComponent().getContainerEntity().getComponent(DecorationInventoryComponent.class);
							if (decorationInventoryComponent != null && decorationInventoryComponent.getDecorationEntities().stream().anyMatch(e -> e.equals(message.targetEntity))) {
								messageDispatcher.dispatchMessage(MessageType.DAMAGE_FURNITURE, new FurnitureDamagedMessage(
										message.targetEntity.getLocationComponent().getContainerEntity(), OXIDISED, null,
										message.oxidisedMaterial.getColor(), null
								));
							}
						}

					}
				} else if (entityAttributes instanceof FurnitureEntityAttributes) {
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entityAttributes;
					if (attributes.getPrimaryMaterial().equals(message.oxidisedMaterial)) {
						// Only destroy if parent primary material is that which oxidised

						messageDispatcher.dispatchMessage(MessageType.DAMAGE_FURNITURE, new FurnitureDamagedMessage(
								message.targetEntity, OXIDISED, message.oxidisedMaterial,
								message.oxidisedMaterial.getColor(), null
						));


						showNotificationOxidisationDestroyedSomething(message.targetEntity);

					}
				} else {
					Logger.error("Not yet implemented: destroy parent due to oxidisation for " + entityAttributes.getClass().getSimpleName());
				}
				break;
			default:
				Logger.error("Not yet implemented: effect of oxidisation " + message.oxidisedMaterial.getOxidisation().getEffect());
		}


		return true;
	}

	private void handleFindButcherableCorpse(RequestCorpseMessage requestCorpseMessage) {
		MapTile requesterTile = gameContext.getAreaMap().getTile(requestCorpseMessage.requesterPosition);
		if (requesterTile == null) {
			return;
		}
		int requesterRegionId = requesterTile.getRegionId();

		Map<Float, Entity> eligibleCorpsesByDistance = new TreeMap<>();

		for (Entity deadCreatureEntity : creatureTracker.getDead()) {
			MapTile corpseTile = gameContext.getAreaMap().getTile(deadCreatureEntity.getLocationComponent().getWorldPosition());
			if (corpseTile == null || corpseTile.getRegionId() != requesterRegionId) {
				continue;
			}

			ItemAllocationComponent itemAllocationComponent = deadCreatureEntity.getComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent == null || itemAllocationComponent.getNumUnallocated() <= 0) {
				continue;
			}

			// else this is unallocated and in same region
			float distanceToCorpse = deadCreatureEntity.getLocationComponent().getWorldOrParentPosition().dst2(requestCorpseMessage.requesterPosition);
			eligibleCorpsesByDistance.put(distanceToCorpse, deadCreatureEntity);
		}

		if (!eligibleCorpsesByDistance.isEmpty()) {
			requestCorpseMessage.callback.corpseFound(eligibleCorpsesByDistance.values().iterator().next());
		}
	}

	private void showNotificationOxidisationDestroyedSomething(Entity targetEntity) {
		Notification notification = new Notification(NotificationType.OXIDISATION_DESTRUCTION, null, new Selectable(targetEntity, 0));
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, notification);
	}

	private void dropEquippedItems(Entity entity, Vector2 entityPosition) {
		HaulingComponent haulingComponent = entity.getComponent(HaulingComponent.class);
		if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
			Entity hauledEntity = haulingComponent.getHauledEntity();
			haulingComponent.clearHauledEntity();
			placeOnGround(hauledEntity, entityPosition);
			entity.removeComponent(HaulingComponent.class);
		}
		EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null) {
			for (Entity equipmentEntity : equippedItemComponent.clearHeldEquipment()) {
				placeOnGround(equipmentEntity, entityPosition);
			}
			entity.removeComponent(EquippedItemComponent.class);
		}
	}

	private void placeOnGround(Entity hauledEntity, Vector2 position) {
		if (hauledEntity.getType().equals(ITEM)) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) hauledEntity.getPhysicalEntityComponent().getAttributes();
			itemAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
		}

		hauledEntity.getLocationComponent().setWorldPosition(position, false);
		hauledEntity.getLocationComponent().setFacing(DOWN.toVector2());
		hauledEntity.getOrCreateComponent(FactionComponent.class).setFaction(SETTLEMENT);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, hauledEntity);
	}

	private float slightRotation() {
		float rotationAmount = gameContext.getRandom().nextFloat() * 15f;
		if (gameContext.getRandom().nextBoolean()) {
			rotationAmount *= -1f;
		}
		return rotationAmount;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
