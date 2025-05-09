package technology.rocketjump.mountaincore.cooking;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.entities.EntityUpdater;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CollectItemFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.tags.CollectItemsBehaviourTag;
import technology.rocketjump.mountaincore.entities.tags.ConstructionOverrideTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.CookingCompleteMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;
import technology.rocketjump.mountaincore.rooms.components.behaviour.KitchenBehaviour;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.rooms.constructions.ConstructionState;
import technology.rocketjump.mountaincore.rooms.constructions.ConstructionStore;
import technology.rocketjump.mountaincore.settlement.SettlementFurnitureTracker;
import technology.rocketjump.mountaincore.settlement.SettlementItemTracker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible for moving cooked food (currently only in cauldrons) from any kitchen
 * To any construction site that requires an item to contain edible contents
 */
@Singleton
public class KitchenManager implements Telegraph, Updatable {

	private final MessageDispatcher messageDispatcher;
	private final ConstructionStore constructionStore;
	private final Skill cookingProfession;
	private final SettlementFurnitureTracker settlementFurnitureTracker;
	private final JobType haulingJobType;
	private final SettlementItemTracker settlementItemTracker;

	private GameContext gameContext;
	private float timeSinceLastUpdate = 0f;


	@Inject
	public KitchenManager(MessageDispatcher messageDispatcher, ConstructionStore constructionStore,
						  SkillDictionary skillDictionary, SettlementFurnitureTracker settlementFurnitureTracker,
						  JobTypeDictionary jobTypeDictionary, ConstantsRepo constantsRepo, SettlementItemTracker settlementItemTracker) {
		this.messageDispatcher = messageDispatcher;
		this.constructionStore = constructionStore;
		this.settlementItemTracker = settlementItemTracker;
		this.cookingProfession = skillDictionary.getByName(constantsRepo.getSettlementConstants().getKitchenProfession());
		this.settlementFurnitureTracker = settlementFurnitureTracker;
		this.haulingJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getHaulingJobType());

		messageDispatcher.addListener(this, MessageType.COOKING_COMPLETE);
		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY);
		messageDispatcher.addListener(this, MessageType.HAULING_ALLOCATION_CANCELLED);
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (gameContext != null && timeSinceLastUpdate > EntityUpdater.TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS) {
			timeSinceLastUpdate = 0f;

			// Look for unallocated constructions of REQUIRES_EDIBLE_LIQUID tagged furniture
			for (Construction construction : constructionStore.getAll()) {
				// Only those in SELECTING_MATERIALS state have not yet been allocated to
				if (construction.getConstructionOverrideSettings().contains(ConstructionOverrideTag.ConstructionOverrideSetting.REQUIRES_EDIBLE_LIQUID) && construction.getState().equals(ConstructionState.SELECTING_MATERIALS)) {
					// FIXME this assumes that a REQUIRES_EDIBLE_LIQUID construction only has a construction requirement of a single entity/item
					Entity matchingEntity = getMatchingInput(construction.getPrimaryMaterialType(), construction.getRequirements());
					if (matchingEntity != null) {
						// found a match

						Job haulingJob = createHaulingJob(matchingEntity, construction);
						if (haulingJob != null) {
							haulingJob.setUninterruptible(true); //ensure the furniture gets there!
							construction.getIncomingHaulingAllocations().add(haulingJob.getHaulingAllocation()); // To track when allocation is cancelled

							// Match priority of hauling job to priority of room it comes from
							MapTile matchingEntityTile = gameContext.getAreaMap().getTile(matchingEntity.getLocationComponent().getWorldPosition());
							if (matchingEntityTile != null && matchingEntityTile.getRoomTile() != null && matchingEntityTile.getRoomTile().getRoom() != null) {
								KitchenBehaviour kitchenBehaviour = matchingEntityTile.getRoomTile().getRoom().getComponent(KitchenBehaviour.class);
								if (kitchenBehaviour != null) {
									haulingJob.setJobPriority(kitchenBehaviour.getPriority());
									construction.setPriority(kitchenBehaviour.getPriority(), messageDispatcher);
								}
							}

							messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);

							// TODO would all of this be better served by adding a new FurnitureBehaviour to the cauldron containing soup?

							gameContext.getSettlementState().furnitureHoldingCompletedCooking.remove(matchingEntity.getId());
							construction.setState(ConstructionState.WAITING_FOR_COMPLETION);
						}
					}
				}
			}

			List<Entity> furnitureWithItemsToMove = new LinkedList<>();
			for (Entity furnitureHoldingCooking : gameContext.getSettlementState().furnitureHoldingCompletedCooking.values()) {
				InventoryComponent inventoryComponent = furnitureHoldingCooking.getComponent(InventoryComponent.class);
				if (!inventoryComponent.isEmpty()) {
					InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.getInventoryEntries().iterator().next();
					ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						furnitureWithItemsToMove.add(furnitureHoldingCooking);
					}
				}
			}
			// Help along furniture with CollectItemsBehaviour to move completed cooking out
			setupHaulingToCollectionFurniture(furnitureWithItemsToMove);

		}
	}

	private void setupHaulingToCollectionFurniture(List<Entity> furnitureWithItemsToMove) {
		for (Entity collectItemsFurniture : settlementFurnitureTracker.findByTag(CollectItemsBehaviourTag.class, false)) {
			if (collectItemsFurniture.getBehaviourComponent() instanceof CollectItemFurnitureBehaviour collectItemFurnitureBehaviour) {

				for (Entity cookingFurniture : new ArrayList<>(furnitureWithItemsToMove)) {
					InventoryComponent inventoryComponent = cookingFurniture.getComponent(InventoryComponent.class);
					InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.getInventoryEntries().iterator().next();

					if (collectItemFurnitureBehaviour.canAccept(inventoryEntry.entity)) {


						ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
						ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);


						int numToAllocate = Math.min(itemAllocationComponent.getNumUnallocated(), attributes.getItemType().getMaxHauledAtOnce());
						HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(numToAllocate, inventoryEntry.entity, cookingFurniture)
								.toEntity(collectItemsFurniture);

						collectItemFurnitureBehaviour.createHaulingJobForAllocation(collectItemFurnitureBehaviour.getMatch(attributes), haulingAllocation);
						furnitureWithItemsToMove.remove(cookingFurniture);
					}
				}
			}
		}
	}

	private Job createHaulingJob(Entity matchingEntity, Construction construction) {
		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(construction.getPriority());
		haulingJob.setTargetId(matchingEntity.getId());
		haulingJob.setRequiredProfession(cookingProfession);

		if (matchingEntity.getType().equals(EntityType.FURNITURE)) {
			haulingJob.setHaulingAllocation(HaulingAllocationBuilder.createToHaulFurniture(matchingEntity)
					.toConstruction(construction));

			FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(matchingEntity, gameContext.getAreaMap());
			if (navigableWorkspace == null) {
				Logger.warn("Could not find navigable workspace of " + matchingEntity.getPhysicalEntityComponent().getAttributes().toString());
				return null;
			} else {
				haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
			}
		} else if (matchingEntity.getType().equals(EntityType.ITEM)) {
			ItemAllocationComponent itemAllocationComponent = matchingEntity.getComponent(ItemAllocationComponent.class);
			haulingJob.setHaulingAllocation(HaulingAllocationBuilder.createWithItemAllocation(itemAllocationComponent.getNumUnallocated(), matchingEntity, construction.getEntity())
					.toConstruction(construction));

			haulingJob.setJobLocation(VectorUtils.toGridPoint(matchingEntity.getLocationComponent().getWorldOrParentPosition()));
		} else {
			Logger.error("Unrecognised entity type in " + this.getClass().getSimpleName());
			return null;
		}

		return haulingJob;
	}

	/**
	 * This only handles REQUIRES_EDIBLE_LIQUID for now
	 */
	private Entity getMatchingInput(GameMaterialType primaryMaterialType, List<QuantifiedItemTypeWithMaterial> constructionRequirements) {
		if (constructionRequirements.size() != 1) {
			Logger.error("Not expecting list of requirements with 0 or more than 1 item");
			return null;
		}
		QuantifiedItemTypeWithMaterial constructionRequirement = constructionRequirements.get(0);

		for (Entity furnitureEntity : gameContext.getSettlementState().furnitureHoldingCompletedCooking.values()) {
			LiquidContainerComponent liquidContainerComponent = furnitureEntity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null) {
				GameMaterial liquidMaterial = liquidContainerComponent.getTargetLiquidMaterial();
				if (liquidMaterial.isEdible()) {

					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
					List<QuantifiedItemType> furnitureRequirements = attributes.getFurnitureType().getRequirements().get(primaryMaterialType);
					if (furnitureRequirements != null) {
						if (furnitureRequirements.size() != 1) {
							Logger.error("Not expecting list of requirements with 0 or more than 1 item");
						} else {
							QuantifiedItemType furnitureRequirement = furnitureRequirements.get(0);

							if (constructionRequirement.getItemType().equals(furnitureRequirement.getItemType())) {
								// Everything matches, good to go
								return furnitureEntity;
							}
						}
					}  // else furniture is of wrong material type

				}
			}
		}

		// Try any lost items containing edible liquid
		for (Entity itemEntity : settlementItemTracker.getItemsByType(constructionRequirement.getItemType(), true)) {
			LiquidContainerComponent liquidContainerComponent = itemEntity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null) {
				if (liquidContainerComponent.getTargetLiquidMaterial().isEdible() && liquidContainerComponent.getNumUnallocated() > 0) {
					return itemEntity;
				}
			}
		}

		return null;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.COOKING_COMPLETE: {
				CookingCompleteMessage cookingCompleteMessage = (CookingCompleteMessage) msg.extraInfo;
				gameContext.getSettlementState().furnitureHoldingCompletedCooking.put(cookingCompleteMessage.targetFurnitureEntity.getId(), cookingCompleteMessage.targetFurnitureEntity);
				return true;
			}
			case MessageType.DESTROY_ENTITY: {
				Entity entity = (Entity) msg.extraInfo;
				gameContext.getSettlementState().furnitureHoldingCompletedCooking.remove(entity.getId());
				return false; // Not the primary handler for this message type
			}
			case MessageType.HAULING_ALLOCATION_CANCELLED: {
				HaulingAllocation allocation = (HaulingAllocation) msg.extraInfo;
				if (allocation.getHauledEntityType().equals(EntityType.FURNITURE)) {
					Entity hauledEntity = gameContext.getEntities().get(allocation.getHauledEntityId());
					if (hauledEntity != null) {
						LiquidContainerComponent liquidContainerComponent = hauledEntity.getComponent(LiquidContainerComponent.class);
						if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null
								&& liquidContainerComponent.getTargetLiquidMaterial().isEdible()) {
							// If this is a liquid container holding an edible liquid, it was probably some cooked soup to be hauled
							if (hauledEntity.getType().equals(EntityType.FURNITURE)) {
								gameContext.getSettlementState().furnitureHoldingCompletedCooking.put(hauledEntity.getId(), hauledEntity);
							} else {
								Logger.warn("Hauling of item with completed cooking cancelled, currently this will be lost and not put back in furnitureHoldingCompletedCooking");
							}
							return true;
						}
					}
				}
				return false; // Not the primary handler for this message type
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;

		// Nothing to do, just look in gameContext.settlementState.furnitureHoldingCompletedCooking
	}

	@Override
	public void clearContextRelatedState() {
		gameContext = null;
		timeSinceLastUpdate = 0f;
	}
}
