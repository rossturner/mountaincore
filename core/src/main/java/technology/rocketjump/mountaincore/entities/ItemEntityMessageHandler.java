package technology.rocketjump.mountaincore.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.components.*;
import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.item.AmmoType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.*;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.production.AbstractStockpile;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.StockpileRoomComponent;
import technology.rocketjump.mountaincore.rooms.tags.StockpileTag;
import technology.rocketjump.mountaincore.settlement.SettlementItemTracker;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ItemEntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final ItemEntityFactory itemEntityFactory;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final SettlementItemTracker settlementItemTracker;
	private final JobStore jobStore;
	private GameContext gameContext;
	private JobType haulingJobType;
	private final ItemTypeDictionary itemTypeDictionary;
	public final StockpileGroupDictionary stockpileGroupDictionary;

	@Inject
	public ItemEntityMessageHandler(MessageDispatcher messageDispatcher,
									ItemEntityFactory itemEntityFactory, GameMaterialDictionary gameMaterialDictionary,
									JobStore jobStore, JobTypeDictionary jobTypeDictionary,
									SettlementItemTracker settlementItemTracker, ItemTypeDictionary itemTypeDictionary, StockpileGroupDictionary stockpileGroupDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityFactory = itemEntityFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.jobStore = jobStore;
		this.haulingJobType = jobTypeDictionary.getByName("HAULING");
		this.settlementItemTracker = settlementItemTracker;
		this.itemTypeDictionary = itemTypeDictionary;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		messageDispatcher.addListener(this, MessageType.ITEM_CREATION_REQUEST);
		messageDispatcher.addListener(this, MessageType.REQUEST_ENTITY_HAULING);
		messageDispatcher.addListener(this, MessageType.REQUEST_HAULING_ALLOCATION);
		messageDispatcher.addListener(this, MessageType.LOOKUP_ITEM_TYPE);
		messageDispatcher.addListener(this, MessageType.LOOKUP_ITEM_TYPES_BY_TAG_CLASS);
		messageDispatcher.addListener(this, MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE);
		messageDispatcher.addListener(this, MessageType.CANCEL_ITEM_ALLOCATION);
		messageDispatcher.addListener(this, MessageType.LOOKUP_ITEM_TYPES_BY_STOCKPILE_GROUP);
		messageDispatcher.addListener(this, MessageType.CHECK_ITEM_AVAILABILITY);
		messageDispatcher.addListener(this, MessageType.GET_ITEMS);
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
				return handle((LookupItemTypeMessage)msg.extraInfo);
			}
			case MessageType.LOOKUP_ITEM_TYPES_BY_TAG_CLASS: {
				return handle((LookupItemTypesByTagClassMessage)msg.extraInfo);
			}
			case MessageType.LOOKUP_ITEM_TYPES_BY_STOCKPILE_GROUP: {
				return handleLookupByStockpileGroup((LookupItemTypeMessage)msg.extraInfo);
			}
			case MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE: {
				return handle((ItemMaterialSelectionMessage)msg.extraInfo);
			}
			case MessageType.CANCEL_ITEM_ALLOCATION: {
				ItemAllocation itemAllocation = (ItemAllocation) msg.extraInfo;
				cancelItemAllocation(itemAllocation);
				return true;
			}
			case MessageType.CHECK_ITEM_AVAILABILITY: {
				return handle((MessageType.CheckItemAvailabilityMessage)msg.extraInfo);
			}
			case MessageType.GET_ITEMS: {
				return handle((MessageType.GetItemsMessage)msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(LookupItemTypesByTagClassMessage message) {
		message.callback.itemTypesFound(itemTypeDictionary.getByTagClass(message.tagClass));
		return true;
	}

	private boolean handleLookupByStockpileGroup(LookupItemTypeMessage message) {
		StockpileGroup stockpileGroup = stockpileGroupDictionary.getByName(message.typeName);
		message.callback.itemTypesFound(itemTypeDictionary.getByStockpileGroup(stockpileGroup));
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

	private boolean handle(LookupItemTypeMessage itemTypeLookupItemTypeMessage) {
		AmmoType ammoType = EnumUtils.getEnum(AmmoType.class, itemTypeLookupItemTypeMessage.typeName);
		if (ammoType != null) {
			itemTypeLookupItemTypeMessage.callback.itemTypesFound(new ArrayList<>(itemTypeDictionary.getByAmmoType(ammoType)));
		} else {
			ItemType itemType = itemTypeDictionary.getByName(itemTypeLookupItemTypeMessage.typeName);
			itemTypeLookupItemTypeMessage.callback.itemTypeFound(Optional.ofNullable(itemType));
		}
		return true;
	}

	private boolean handle(MessageType.GetItemsMessage message) {
		final List<Entity> items;
		if (message.material() != null) {
			items = settlementItemTracker.getItemsByTypeAndMaterial(message.itemType(), message.material(), false);
		} else {
			items = settlementItemTracker.getItemsByType(message.itemType(), true);
		}
		message.callback().accept(items);
		return true;
	}

	private boolean handle(RequestHaulingAllocationMessage message) {
		int requesterRegionId = gameContext.getAreaMap().getTile(message.requesterPosition).getRegionId();
		List<Entity> unallocatedItems = new ArrayList<>();
		Set<ItemType> requiredItemTypes = new HashSet<>();
		if (message.requiredItemType != null) {
			requiredItemTypes.add(message.requiredItemType);
		}
		if (message.requiredItemTypeTag != null) {
			messageDispatcher.dispatchMessage(MessageType.LOOKUP_ITEM_TYPES_BY_TAG_CLASS, new LookupItemTypesByTagClassMessage(
					message.requiredItemTypeTag, requiredItemTypes::addAll));
		}


		for (ItemType requiredItemType : requiredItemTypes) {
			if (message.requiredMaterial != null) {
				unallocatedItems.addAll(settlementItemTracker.getItemsByTypeAndMaterial(requiredItemType, message.requiredMaterial, true));
			} else {
				unallocatedItems.addAll(settlementItemTracker.getItemsByType(requiredItemType, true));
			}
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
		List<Entity> itemsByType = settlementItemTracker.getItemsByType(itemMaterialSelectionMessage.itemType, true);
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
				itemMaterialSelectionMessage.callback.accept(mostCommonMaterial);
			} else {
				itemMaterialSelectionMessage.callback.accept(null);
			}
		} else {
			itemMaterialSelectionMessage.callback.accept(null);
		}
		return true;
	}

	private boolean handle(MessageType.CheckItemAvailabilityMessage message) {
		if (message.requirement().getMaterial() != null) {
			List<Entity> items = settlementItemTracker.getItemsByType(message.requirement().getItemType(), true);
			message.callback().accept(countAvailability(items));
		} else {
			List<Entity> items = settlementItemTracker.getItemsByTypeAndMaterial(message.requirement().getItemType(), message.requirement().getMaterial(), true);
			message.callback().accept(countAvailability(items));
		}
		return true;
	}

	private Integer countAvailability(List<Entity> items) {
		return items.stream()
				.map(e -> e.getComponent(ItemAllocationComponent.class).getNumUnallocated())
				.reduce(0, Integer::sum);
	}

	public static HaulingAllocation findStockpileAllocation(TiledMap areaMap, Entity entity, Entity requestingEntity, MessageDispatcher messageDispatcher) {
		Vector2 entityPosition = entity.getLocationComponent().getWorldOrParentPosition();
		int sourceRegionId = areaMap.getTile(entityPosition).getRegionId();
		Map<JobPriority, Map<Float, AbstractStockpile>> stockpilesByDistanceByPriority = new EnumMap<>(JobPriority.class);
		for (JobPriority jobPriority : JobPriority.values()) {
			stockpilesByDistanceByPriority.put(jobPriority, new TreeMap<>(Comparator.comparingInt(o -> (int) (o * 10))));
		}

		JobPriority currentStockpilePriority = getStockpilePriority(entity, entityPosition, areaMap);

		messageDispatcher.dispatchMessage(MessageType.GET_ROOMS_BY_COMPONENT, new GetRoomsByComponentMessage(StockpileRoomComponent.class, rooms -> {
			for (Room room : rooms) {
				StockpileRoomComponent stockpileRoomComponent = room.getComponent(StockpileRoomComponent.class);
				if (stockpileRoomComponent.getStockpileSettings().canHold(entity)) {
					int roomRegionId = room.getRoomTiles().values().iterator().next().getTile().getRegionId();
					if (sourceRegionId == roomRegionId) {
						Map<Float, AbstractStockpile> byDistance = stockpilesByDistanceByPriority.get(stockpileRoomComponent.getPriority());
						byDistance.put(entityPosition.dst2(room.getAvgWorldPosition()), stockpileRoomComponent.getStockpile());
					}
				}
			}
		}));

		messageDispatcher.dispatchMessage(MessageType.GET_FURNITURE_BY_TAG, new GetFurnitureByTagMessage(StockpileTag.class, furniture -> {
			for (Entity pieceOfFurniture : furniture) {
				FurnitureStockpileComponent stockpileComponent = pieceOfFurniture.getComponent(FurnitureStockpileComponent.class);
				if (stockpileComponent != null && stockpileComponent.getStockpileSettings().canHold(entity) && pieceOfFurniture.getLocationComponent().getWorldPosition() != null) {
					Map<Float, AbstractStockpile> byDistance = stockpilesByDistanceByPriority.get(stockpileComponent.getPriority());
					byDistance.put(entityPosition.dst2(pieceOfFurniture.getLocationComponent().getWorldPosition()), stockpileComponent.getStockpile());
				}
			}
		}));

		for (int i = 0; i < currentStockpilePriority.ordinal(); i++) {
			JobPriority priority = JobPriority.values()[i];

			Map<Float, AbstractStockpile> byDistance = stockpilesByDistanceByPriority.getOrDefault(priority, Collections.emptyMap());
			for (AbstractStockpile stockpile : byDistance.values()) {
				HaulingAllocation haulingAllocation = stockpile.requestAllocation(entity, areaMap, requestingEntity);
				if (haulingAllocation != null) {
					return haulingAllocation;
				}
			}
		}

		return null;
	}

	public static Job createHaulingJob(HaulingAllocation haulingAllocation, Entity itemEntity, JobType haulingJobType, JobPriority jobPriority) {
		Job haulingJob = new Job(haulingJobType);
		haulingJob.setTargetId(itemEntity.getId());
		haulingJob.setJobLocation(VectorUtils.toGridPoint(itemEntity.getLocationComponent().getWorldOrParentPosition()));
		haulingJob.setHaulingAllocation(haulingAllocation);
		haulingJob.setJobPriority(jobPriority);
		return haulingJob;
	}

	private boolean handle(RequestHaulingMessage message) {
		HaulingAllocation haulingAllocation = findStockpileAllocation(gameContext.getAreaMap(), message.getEntityToBeMoved(), message.requestingEntity, messageDispatcher);

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
			if (containerEntity != null && EntityType.FURNITURE == containerEntity.getType()) {
				FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(containerEntity, gameContext.getAreaMap());
				if (navigableWorkspace != null) {
					haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
					haulingJob.setJobState(JobState.ASSIGNABLE);
				} else if (containerEntity.getComponent(FurnitureStockpileComponent.class) != null ||
						!((FurnitureEntityAttributes)containerEntity.getPhysicalEntityComponent().getAttributes()).getFurnitureType().isBlocksMovement()) {
					haulingJob.setJobLocation(VectorUtils.toGridPoint(containerEntity.getLocationComponent().getWorldPosition()));
					haulingJob.setJobState(JobState.ASSIGNABLE);
				} else {
					Logger.error("Item created but not accessible to collect - investigate and fix");
					messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, haulingAllocation.getItemAllocation());
					return true;
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
		ItemType itemType = message.getRequiredItemType();
		ItemEntityAttributes attributes = message.getAttributes();
		if (attributes == null) {
			attributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
			attributes.setItemType(itemType);

			for (GameMaterialType requiredMaterialType : itemType.getMaterialTypes()) {
				List<GameMaterial> materialsToPickFrom;
				if (requiredMaterialType.equals(itemType.getPrimaryMaterialType()) && !itemType.getSpecificMaterials().isEmpty()) {
					materialsToPickFrom = itemType.getSpecificMaterials();
				} else {
					materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
							.filter(GameMaterial::isUseInRandomGeneration)
							.collect(Collectors.toList());
				}

				if (!materialsToPickFrom.isEmpty()) {
					GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
					attributes.setMaterial(material);
				}
			}
			attributes.setQuantity(1);
		}

		Entity item = itemEntityFactory.create(attributes, null, message.isAddToGameContext(), gameContext, Faction.SETTLEMENT);
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
		if (entity.getLocationComponent().getContainerEntity() != null) {
			FurnitureStockpileComponent component = entity.getLocationComponent().getContainerEntity().getComponent(FurnitureStockpileComponent.class);
			if (component != null) {
				return component.getPriority();
			}
		}
		if (tile.getRoomTile() != null) {
			Room room = tile.getRoomTile().getRoom();
			StockpileRoomComponent stockpileRoomComponent = room.getComponent(StockpileRoomComponent.class);
			if (stockpileRoomComponent != null && stockpileRoomComponent.getStockpileSettings().canHold(entity)) {
				return stockpileRoomComponent.getPriority();
			}
		}
		return JobPriority.DISABLED;
	}
}
