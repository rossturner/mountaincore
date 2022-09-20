package technology.rocketjump.saul.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.item.AmmoType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.jobs.model.JobState;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;
import technology.rocketjump.saul.production.StockpileAllocationResponse;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.rooms.components.StockpileComponent;
import technology.rocketjump.saul.settlement.ItemTracker;

import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

@Singleton
public class ItemEntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final ItemEntityFactory itemEntityFactory;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final RoomStore roomStore;
	private final ItemTracker itemTracker;
	private final JobStore jobStore;
	private GameContext gameContext;
	private JobType haulingJobType;
	private final ItemTypeDictionary itemTypeDictionary;

	@Inject
	public ItemEntityMessageHandler(MessageDispatcher messageDispatcher,
									ItemEntityFactory itemEntityFactory, GameMaterialDictionary gameMaterialDictionary,
									JobStore jobStore, JobTypeDictionary jobTypeDictionary, RoomStore roomStore,
									ItemTracker itemTracker, ItemTypeDictionary itemTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityFactory = itemEntityFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.jobStore = jobStore;
		this.haulingJobType = jobTypeDictionary.getByName("HAULING");
		this.roomStore = roomStore;
		this.itemTracker = itemTracker;
		this.itemTypeDictionary = itemTypeDictionary;
		messageDispatcher.addListener(this, MessageType.ITEM_CREATION_REQUEST);
		messageDispatcher.addListener(this, MessageType.REQUEST_ENTITY_HAULING);
		messageDispatcher.addListener(this, MessageType.REQUEST_HAULING_ALLOCATION);
		messageDispatcher.addListener(this, MessageType.LOOKUP_ITEM_TYPE);
		messageDispatcher.addListener(this, MessageType.LOOKUP_ITEM_TYPES_BY_TAG_CLASS);
		messageDispatcher.addListener(this, MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE);
		messageDispatcher.addListener(this, MessageType.CANCEL_ITEM_ALLOCATION);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ITEM_CREATION_REQUEST: {
				return handle((ItemCreationRequestMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_ENTITY_HAULING: {
				return handle((RequestHaulingMessage)msg.extraInfo);
			}
			case MessageType.REQUEST_HAULING_ALLOCATION: {
				return handle((RequestHaulingAllocationMessage)msg.extraInfo);
			}
			case MessageType.LOOKUP_ITEM_TYPE: {
				return handle((LookupMessage)msg.extraInfo);
			}
			case MessageType.LOOKUP_ITEM_TYPES_BY_TAG_CLASS: {
				return handle((LookupItemTypesByTagClassMessage)msg.extraInfo);
			}
			case MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE: {
				return handle((ItemMaterialSelectionMessage)msg.extraInfo);
			}
			case MessageType.CANCEL_ITEM_ALLOCATION: {
				ItemAllocation itemAllocation = (ItemAllocation) msg.extraInfo;
				cancelItemAllocation(itemAllocation);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(LookupItemTypesByTagClassMessage message) {
		message.callback.itemTypesFound(itemTypeDictionary.getByTagClass(message.tagClass));
		return true;
	}

	private void cancelItemAllocation(ItemAllocation itemAllocation) {
		Entity itemEntity = gameContext.getEntities().get(itemAllocation.getTargetItemEntityId());
		if (itemEntity != null) {
			ItemAllocationComponent itemAllocationComponent = itemEntity.getComponent(ItemAllocationComponent.class);
			itemAllocationComponent.cancel(itemAllocation);
		}

		// interrupt any settlers using this item
		if (itemAllocation.getRelatedHaulingAllocationId() != null) {
			Job relatedJob = jobStore.getByHaulingAllocationId(itemAllocation.getRelatedHaulingAllocationId());
			if (relatedJob != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, relatedJob);
			}
		}

	}

	private boolean handle(LookupMessage itemTypeLookupMessage) {
		if (itemTypeLookupMessage.entityType.equals(EntityType.ITEM)) {
			AmmoType ammoType = EnumUtils.getEnum(AmmoType.class, itemTypeLookupMessage.typeName);
			if (ammoType != null) {
				itemTypeLookupMessage.callback.itemTypesFound(new ArrayList<>(itemTypeDictionary.getByAmmoType(ammoType)));
			} else {
				ItemType itemType = itemTypeDictionary.getByName(itemTypeLookupMessage.typeName);
				itemTypeLookupMessage.callback.itemTypeFound(Optional.ofNullable(itemType));
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean handle(RequestHaulingAllocationMessage message) {
		int requesterRegionId = gameContext.getAreaMap().getTile(message.requesterPosition).getRegionId();
		List<Entity> unallocatedItems;
		if (message.requiredMaterial != null) {
			unallocatedItems = itemTracker.getItemsByTypeAndMaterial(message.requiredItemType, message.requiredMaterial, true);
		} else {
			unallocatedItems = itemTracker.getItemsByType(message.requiredItemType, true);
		}

		unallocatedItems.sort(new NearestDistanceSorter(message.requesterPosition));

		for (Entity unallocatedItem : unallocatedItems) {
			MapTile itemTile = gameContext.getAreaMap().getTile(unallocatedItem.getLocationComponent().getWorldOrParentPosition());
			if (itemTile == null || itemTile.getRegionId() != requesterRegionId) {
				// Item not found or in different region
				continue;
			}

			LiquidContainerComponent liquidContainerComponent = unallocatedItem.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
				if (message.requiredContainedLiquid == null) {
					// Not requesting item to contain a specific liquid so don't use this one
					continue;
				} else if (!message.requiredContainedLiquid.equals(liquidContainerComponent.getTargetLiquidMaterial()) || liquidContainerComponent.getNumUnallocated() <= 0) {
					continue;
				}
			} else if (message.requiredContainedLiquid != null) {
				// No liquid container component or quantity and this request specifies a liquid
				continue;
			}

			Entity containerEntity = unallocatedItem.getLocationComponent().getContainerEntity();
			Long requestingEntityId = null;
			if (message.requestingEntity != null) {
				requestingEntityId = message.requestingEntity.getId();
			}
			if (containerEntity != null) {
				if (message.includeFromFurniture && !Objects.equals(containerEntity.getId(), requestingEntityId)) {
					if (!containerEntity.getType().equals(EntityType.FURNITURE)) {
						Logger.warn("Not yet implemented: Requesting item from non-furniture container");
						continue;
					}
				} else {
					// This request does not want items from other containers
					continue;
				}
			}

			ItemEntityAttributes attributes = (ItemEntityAttributes) unallocatedItem.getPhysicalEntityComponent().getAttributes();

			ItemAllocationComponent itemAllocationComponent = unallocatedItem.getOrCreateComponent(ItemAllocationComponent.class);

			int numToAllocate = Math.min(itemAllocationComponent.getNumUnallocated(), attributes.getItemType().getMaxHauledAtOnce());
			if (message.maxAmountRequired != null) {
				numToAllocate = Math.min(numToAllocate, message.maxAmountRequired);
			}
			Entity requestingEntity = message.requestingEntity != null ? message.requestingEntity : unallocatedItem;
			HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(numToAllocate, unallocatedItem, requestingEntity)
					.toEntity(requestingEntity);
			if (liquidContainerComponent != null && message.requiredContainedLiquid != null) {
				LiquidAllocation liquidAllocation = liquidContainerComponent.createAllocationDueToParentHauling(liquidContainerComponent.getNumUnallocated(), message.requestingEntity);
				haulingAllocation.setLiquidAllocation(liquidAllocation);
			}
			message.allocationCallback.allocationFound(haulingAllocation);
			return true;
		}

		message.allocationCallback.allocationFound(null);
		return true;
	}

	private boolean handle(ItemMaterialSelectionMessage itemMaterialSelectionMessage) {
		List<Entity> itemsByType = itemTracker.getItemsByType(itemMaterialSelectionMessage.itemType, true);
		if (!itemsByType.isEmpty()) {
			// Select most popular material available in this resource
			Map<GameMaterial, Integer> availabilityMap = new HashMap<>();
			GameMaterial mostCommonMaterial = null;
			int mostCommonQuantity = 0;
			for (Entity entity : itemsByType) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				GameMaterial itemMaterial = attributes.getMaterial(attributes.getItemType().getPrimaryMaterialType());
				ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
				int availableQuantity = itemAllocationComponent.getNumUnallocated();
				Integer quantityOfMaterial = availabilityMap.getOrDefault(itemMaterial, 0);
				quantityOfMaterial += availableQuantity;

				if (quantityOfMaterial > mostCommonQuantity) {
					mostCommonQuantity = quantityOfMaterial;
					mostCommonMaterial = itemMaterial;
				}
				availabilityMap.put(itemMaterial, quantityOfMaterial);
			}

			if (mostCommonQuantity >= itemMaterialSelectionMessage.minimumQuantity) {
				itemMaterialSelectionMessage.callback.materialFound(mostCommonMaterial);
			} else {
				itemMaterialSelectionMessage.callback.materialFound(null);
			}
		} else {
			itemMaterialSelectionMessage.callback.materialFound(null);
		}
		return true;
	}

	public static HaulingAllocation findStockpileAllocation(TiledMap areaMap, Entity entity, RoomStore roomStore, Entity requestingEntity) {
		Vector2 entityPosition = entity.getLocationComponent().getWorldOrParentPosition();
		int sourceRegionId = areaMap.getTile(entityPosition).getRegionId();
		Map<JobPriority, Map<Float, Room>> stockpilesByDistanceByPriority = new EnumMap<>(JobPriority.class);

		JobPriority currentStockpilePriority = getStockpilePriority(entity, entityPosition, areaMap);

		for (Room stockpile : roomStore.getByComponent(StockpileComponent.class)) {
			StockpileComponent stockpileComponent = stockpile.getComponent(StockpileComponent.class);
			if (stockpileComponent.getStockpileSettings().canHold(entity)) {
				int roomRegionId = stockpile.getRoomTiles().values().iterator().next().getTile().getRegionId();
				if (sourceRegionId == roomRegionId) {
					Map<Float, Room> byDistance = stockpilesByDistanceByPriority.computeIfAbsent(stockpileComponent.getPriority(), a -> new TreeMap<>(Comparator.comparingInt(o -> (int) (o * 10))));
					byDistance.put(entityPosition.dst2(stockpile.getAvgWorldPosition()), stockpile);
				}
			}
		}


		for (int i = 0; i < currentStockpilePriority.ordinal(); i++) {
			JobPriority priority = JobPriority.values()[i];

			Map<Float, Room> byDistance = stockpilesByDistanceByPriority.getOrDefault(priority, Collections.emptyMap());
			for (Room room : byDistance.values()) {
				StockpileAllocationResponse stockpileAllocationResponse = room.getComponent(StockpileComponent.class).requestAllocation(entity, areaMap);
				if (stockpileAllocationResponse != null) {
					return HaulingAllocationBuilder.createWithItemAllocation(stockpileAllocationResponse.quantity, entity, requestingEntity)
							.toRoom(room, stockpileAllocationResponse.position);
				}
			}
		}

		return null;
	}

	public static Job createHaulingJob(HaulingAllocation haulingAllocation, Entity itemEntity, JobType haulingJobType, JobPriority jobPriority) {
		Job haulingJob = new Job(haulingJobType);
		haulingJob.setTargetId(itemEntity.getId());
		haulingJob.setJobLocation(toGridPoint(itemEntity.getLocationComponent().getWorldOrParentPosition()));
		haulingJob.setHaulingAllocation(haulingAllocation);
		haulingJob.setJobPriority(jobPriority);
		return haulingJob;
	}

	private boolean handle(RequestHaulingMessage message) {
		HaulingAllocation haulingAllocation = findStockpileAllocation(gameContext.getAreaMap(), message.getEntityToBeMoved(), roomStore, message.requestingEntity);

		if (haulingAllocation == null && message.forceHaulingEvenWithoutStockpile()) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) message.getEntityToBeMoved().getPhysicalEntityComponent().getAttributes();
			ItemAllocationComponent itemAllocationComponent = message.getEntityToBeMoved().getOrCreateComponent(ItemAllocationComponent.class);

			int quantityToAllocate = Math.min(itemAllocationComponent.getNumUnallocated(), itemAttributes.getItemType().getMaxHauledAtOnce());
			if (quantityToAllocate == 0) {
				Logger.error(this.getClass().getSimpleName() + " handled with allocatable quantity of 0, investigate how or why this would happen");
				return true;
			}

			haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(quantityToAllocate, message.getEntityToBeMoved(), message.requestingEntity)
							.toUnspecifiedLocation();
		}

		if (haulingAllocation != null) {
			Job haulingJob = createHaulingJob(haulingAllocation, message.getEntityToBeMoved(), haulingJobType, message.jobPriority);

			Entity itemToBeMoved = message.getEntityToBeMoved();
			Entity containerEntity = itemToBeMoved.getLocationComponent().getContainerEntity();
			if (containerEntity != null) {
				FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(containerEntity, gameContext.getAreaMap());
				if (navigableWorkspace == null) {
					Logger.error("Item created but not accessible to collect - investigate and fix");
					messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, haulingAllocation.getItemAllocation());
					return true;
				} else {
					haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
					haulingJob.setJobState(JobState.ASSIGNABLE);
				}
			}

			if (message.getSpecificProfessionRequired() != null) {
				haulingJob.setRequiredProfession(message.getSpecificProfessionRequired());
			}

			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
			if (message.callback != null) {
				message.callback.jobCreated(haulingJob);
			}
 		}

		return true;
	}

	private boolean handle(ItemCreationRequestMessage message) {
		ItemEntityAttributes attributes = message.getAttributes();
		if (attributes == null) {
			attributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
			attributes.setItemType(message.getRequiredItemType());

			for (GameMaterialType requiredMaterialType : message.getRequiredItemType().getMaterialTypes()) {
				List<GameMaterial> materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
						.filter(GameMaterial::isUseInRandomGeneration)
						.collect(Collectors.toList());
				if (!materialsToPickFrom.isEmpty()) {
					GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
					attributes.setMaterial(material);
				}
			}
			attributes.setQuantity(1);
		}

		Entity item = itemEntityFactory.create(attributes, null, message.isAddToGameContext(), gameContext);
		message.getCallback().entityCreated(item);
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private static final Vector2 FAR_AWAY = new Vector2(-100f, -100f);
	public static class NearestDistanceSorter implements Comparator<Entity> {

		private final Vector2 requesterPosition;

		public NearestDistanceSorter(Vector2 requesterPosition) {
			this.requesterPosition = requesterPosition;
		}

		@Override
		public int compare(Entity o1, Entity o2) {
			Vector2 position1 = o1.getLocationComponent().getWorldOrParentPosition();
			if (position1 == null) {
				position1 = FAR_AWAY;
			}
			Vector2 position2 = o2.getLocationComponent().getWorldOrParentPosition();
			if (position2 == null) {
				position2 = FAR_AWAY;
			}

			return Math.round((position1.dst2(requesterPosition) - position2.dst2(requesterPosition)) * 10000f);
		}
	}

	private static JobPriority getStockpilePriority(Entity entity, Vector2 worldPosition, TiledMap areaMap) {
		MapTile tile = areaMap.getTile(worldPosition);
		if (tile.getRoomTile() != null) {
			Room room = tile.getRoomTile().getRoom();
			StockpileComponent stockpileComponent = room.getComponent(StockpileComponent.class);
			if (stockpileComponent != null && stockpileComponent.getStockpileSettings().canHold(entity)) {
				return stockpileComponent.getPriority();
			}
		}
		return JobPriority.DISABLED;
	}
}
