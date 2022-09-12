package technology.rocketjump.saul.rooms.constructions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.entities.tags.ConstructionOverrideTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.Persistable;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.*;

import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION;
import static technology.rocketjump.saul.entities.tags.ConstructionOverrideTag.ConstructionOverrideSetting.REQUIRES_EDIBLE_LIQUID;
import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.saul.rooms.constructions.ConstructionState.SELECTING_MATERIALS;

public abstract class Construction implements Persistable, SelectableDescription {

	protected long constructionId;

	public long getId() {
		return constructionId;
	}

	public abstract Set<GridPoint2> getTileLocations();

	public abstract ConstructionType getConstructionType();

	protected ConstructionState state = ConstructionState.CLEARING_WORK_SITE;
	protected JobPriority priority = JobPriority.NORMAL;
	protected Job constructionJob;
	protected List<HaulingAllocation> incomingHaulingAllocations = new ArrayList<>();
	protected Map<GridPoint2, ItemAllocation> placedItemAllocations = new HashMap<>();
	protected GameMaterialType primaryMaterialType;
	protected List<QuantifiedItemTypeWithMaterial> requirements = new ArrayList<>(); // Note that material will be null initially
	protected Optional<GameMaterial> playerSpecifiedPrimaryMaterial = Optional.empty();
	protected Set<ConstructionOverrideTag.ConstructionOverrideSetting> constructionOverrideSettings = new HashSet<>();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Construction that = (Construction) o;
		return getId() == that.getId();
	}

	@Override
	public int hashCode() {
		return (int) (getId() ^ (getId() >>> 32));
	}

	public abstract Entity getEntity();

	public JobPriority getPriority() {
		return priority;
	}

	public void setPriority(JobPriority priority, MessageDispatcher messageDispatcher) {
		this.priority = priority;
		if (constructionJob != null) {
			constructionJob.setJobPriority(priority);
		}
		messageDispatcher.dispatchMessage(MessageType.CONSTRUCTION_PRIORITY_CHANGED);
	}

	public void allocationCancelled(HaulingAllocation allocation) {
		incomingHaulingAllocations.remove(allocation);
	}

	public boolean isItemUsedInConstruction(Entity itemEntity) {
		ItemEntityAttributes itemAttributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		for (QuantifiedItemTypeWithMaterial requirement : requirements) {
			if (requirement.getItemType().equals(itemAttributes.getItemType()) &&
					(requirement.getMaterial()) == null || (requirement.getMaterial() != null &&
							requirement.getMaterial().equals(itemAttributes.getMaterial(requirement.getMaterial().getMaterialType())))) {

				if (constructionOverrideSettings.contains(REQUIRES_EDIBLE_LIQUID)) {
					LiquidContainerComponent liquidContainerComponent = itemEntity.getComponent(LiquidContainerComponent.class);
					if (liquidContainerComponent == null || liquidContainerComponent.getTargetLiquidMaterial() == null) {
						return false;
					} else {
						return liquidContainerComponent.getTargetLiquidMaterial().isEdible() && liquidContainerComponent.getLiquidQuantity() > 0;
					}
				} else {
					return true;
				}
			}
		}
		return false;
	}

	public void newItemPlaced(HaulingAllocation haulingAllocation, Entity itemEntity) {
		this.allocationCancelled(haulingAllocation);

		ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();

		ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(attributes.getQuantity(), itemEntity, PLACED_FOR_CONSTRUCTION);
		this.placedItemAllocations.put(haulingAllocation.getTargetPosition(), itemAllocation);
	}

	public void placedItemQuantityIncreased(HaulingAllocation haulingAllocation, Entity existingItem) {
		this.allocationCancelled(haulingAllocation);

		ItemAllocationComponent itemAllocationComponent = existingItem.getOrCreateComponent(ItemAllocationComponent.class);
		ItemEntityAttributes attributes = (ItemEntityAttributes) existingItem.getPhysicalEntityComponent().getAttributes();

		ItemAllocation existingItemAllocation = itemAllocationComponent.getAllocationForPurpose(PLACED_FOR_CONSTRUCTION);
		// Need to check for null in case this is an item left over from a cancelled construction
		if (existingItemAllocation != null) {
			existingItemAllocation.setAllocationAmount(attributes.getQuantity());
		}

		// above itemAllocation should already exist in this.placedItemAllocations
	}

	public Job getConstructionJob() {
		return constructionJob;
	}

	public void setConstructionJob(Job constructionJob) {
		this.constructionJob = constructionJob;
	}

	public List<QuantifiedItemTypeWithMaterial> getRequirements() {
		return requirements;
	}

	public List<HaulingAllocation> getIncomingHaulingAllocations() {
		return incomingHaulingAllocations;
	}

	public Map<GridPoint2, ItemAllocation> getPlacedItemAllocations() {
		return placedItemAllocations;
	}

	public abstract GridPoint2 getPrimaryLocation();

	public GameMaterial getPrimaryMaterial() {
		for (QuantifiedItemTypeWithMaterial requirement : requirements) {
			GameMaterial material = requirement.getMaterial();
			if (material != null && material.getMaterialType().equals(primaryMaterialType)) {
				return material;
			}
		}
		return NULL_MATERIAL;
	}

	public GameMaterialType getPrimaryMaterialType() {
		return primaryMaterialType;
	}

	public ConstructionState getState() {
		return state;
	}

	public void setState(ConstructionState state) {
		this.state = state;
	}

	public boolean isAutoCompleted() {
		return false;
	}

	public Set<ConstructionOverrideTag.ConstructionOverrideSetting> getConstructionOverrideSettings() {
		return constructionOverrideSettings;
	}

	public Optional<GameMaterial> getPlayerSpecifiedPrimaryMaterial() {
		return playerSpecifiedPrimaryMaterial;
	}

	public void setPlayerSpecifiedPrimaryMaterial(Optional<GameMaterial> playerSpecifiedPrimaryMaterial) {
		this.playerSpecifiedPrimaryMaterial = playerSpecifiedPrimaryMaterial;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.constructions.containsKey(getId())) {
			return;
		}
		JSONObject asJson = new JSONObject(true);
		asJson.put("_type", getConstructionType().name());
		asJson.put("id", constructionId);
		asJson.put("priority", priority.name());

		if (!state.equals(ConstructionState.SELECTING_MATERIALS)) {
			asJson.put("state", state.name());
		}
		if (constructionJob != null) {
			constructionJob.writeTo(savedGameStateHolder);
			asJson.put("constructionJob", constructionJob.getJobId());
		}
		if (!incomingHaulingAllocations.isEmpty()) {
			JSONArray allocatedItemsJson = new JSONArray();
			for (HaulingAllocation allocatedItem : incomingHaulingAllocations) {
				allocatedItem.writeTo(savedGameStateHolder);
				allocatedItemsJson.add(allocatedItem.getHaulingAllocationId());
			}
			asJson.put("allocatedItems", allocatedItemsJson);
		}
		if (primaryMaterialType != null) {
			asJson.put("primaryMaterialType", primaryMaterialType.name());
		}

		if (!requirements.isEmpty()) {
			JSONArray requirementsJson = new JSONArray();
			for (QuantifiedItemTypeWithMaterial requirement : requirements) {
				JSONObject requirementJson = new JSONObject(true);
				requirement.writeTo(requirementJson, savedGameStateHolder);
				requirementsJson.add(requirementJson);
			}
			asJson.put("requirements", requirementsJson);
		}
		if (!placedItemAllocations.isEmpty()) {
			JSONArray itemAllocationsJson = new JSONArray();
			for (Map.Entry<GridPoint2, ItemAllocation> entry : placedItemAllocations.entrySet()) {
				JSONObject entryJson = new JSONObject(true);
				entryJson.put("position", JSONUtils.toJSON(entry.getKey()));
				entryJson.put("allocation", entry.getValue().getItemAllocationId());
			}
			asJson.put("placedItemAllocations", itemAllocationsJson);
		}

		if (!constructionOverrideSettings.isEmpty()) {
			JSONArray overrideSettingsJson = new JSONArray();
			for (ConstructionOverrideTag.ConstructionOverrideSetting overrideSetting : constructionOverrideSettings) {
				overrideSettingsJson.add(overrideSetting.name());
			}
			asJson.put("overrides", overrideSettingsJson);
		}

		playerSpecifiedPrimaryMaterial.ifPresent(gameMaterial ->
				asJson.put("playerSpecifiedPrimaryMaterial", gameMaterial.getMaterialName())
		);

		savedGameStateHolder.constructions.put(getId(), this);
		savedGameStateHolder.constructionsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.constructionId = asJson.getLongValue("id");
		if (constructionId == 0L) {
			throw new InvalidSaveException("Could not find construction ID");
		}
		this.state = EnumParser.getEnumValue(asJson, "state", ConstructionState.class, ConstructionState.SELECTING_MATERIALS);
		this.priority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.NORMAL);
		Long constructionJobId = asJson.getLong("constructionJob");
		if (constructionJobId != null) {
			this.constructionJob = savedGameStateHolder.jobs.get(constructionJobId);
			if (this.constructionJob == null) {
				throw new InvalidSaveException("Could not find job with ID " + constructionJobId);
			}
		}

		JSONArray allocateditemsJson = asJson.getJSONArray("allocatedItems");
		if (allocateditemsJson != null) {
			for (int cursor = 0; cursor < allocateditemsJson.size(); cursor++) {
				HaulingAllocation allocation = savedGameStateHolder.haulingAllocations.get(allocateditemsJson.getLongValue(cursor));
				if (allocation == null) {
					throw new InvalidSaveException("Could not find hauling allocation by ID " + allocateditemsJson.getLongValue(cursor));
				} else {
					this.incomingHaulingAllocations.add(allocation);
				}
			}
		}

		JSONArray requirementsJson = asJson.getJSONArray("requirements");
		if (requirementsJson != null) {
			for (int cursor = 0; cursor < requirementsJson.size(); cursor++) {
				JSONObject requirementJson = requirementsJson.getJSONObject(cursor);
				QuantifiedItemTypeWithMaterial requirement = new QuantifiedItemTypeWithMaterial();
				requirement.readFrom(requirementJson, savedGameStateHolder, relatedStores);
				this.requirements.add(requirement);
			}
		}

		JSONArray itemAllocationsjson = asJson.getJSONArray("placedItemAllocations");
		if (itemAllocationsjson != null) {
			for (int cursor = 0; cursor < itemAllocationsjson.size(); cursor++) {
				JSONObject entryJson = itemAllocationsjson.getJSONObject(cursor);
				Long itemAllocationId = entryJson.getLong("allocation");
				ItemAllocation itemAllocation = savedGameStateHolder.itemAllocations.get(itemAllocationId);
				if (itemAllocation != null) {
					GridPoint2 position = JSONUtils.gridPoint2(entryJson.getJSONObject("position"));
					this.placedItemAllocations.put(position, itemAllocation);
				} else {
					throw new InvalidSaveException("Could not find item allocation by ID "  + itemAllocationId);
				}
			}
		}

		JSONArray overrideSettingsJson = asJson.getJSONArray("overrides");
		if (overrideSettingsJson != null) {
			for (int cursor = 0; cursor < overrideSettingsJson.size(); cursor++) {
				ConstructionOverrideTag.ConstructionOverrideSetting override =
						EnumUtils.getEnum(ConstructionOverrideTag.ConstructionOverrideSetting.class, overrideSettingsJson.getString(cursor));
				if (override == null) {
					throw new InvalidSaveException("Could not find override setting by name " + overrideSettingsJson.getString(cursor));
				} else {
					constructionOverrideSettings.add(override);
				}
			}
		}

		this.primaryMaterialType = EnumParser.getEnumValue(asJson, "primaryMaterialType", GameMaterialType.class, null);

		String playerSpecifiedPrimaryMaterialName = asJson.getString("playerSpecifiedPrimaryMaterial");
		if (playerSpecifiedPrimaryMaterialName != null) {
			playerSpecifiedPrimaryMaterial = Optional.ofNullable(relatedStores.gameMaterialDictionary.getByName(playerSpecifiedPrimaryMaterialName));
			if (playerSpecifiedPrimaryMaterial.isEmpty()) {
				throw new InvalidSaveException("Could not find material with name " + playerSpecifiedPrimaryMaterialName);
			}
		}

		savedGameStateHolder.constructions.put(getId(), this);
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		List<I18nText> description = new ArrayList<>(2 + requirements.size());

		description.add(i18nTranslator.getDescription(this)); //TODO: polymorphic
		description.addAll(i18nTranslator.getConstructionStatusDescriptions(this)); //TODO: polymorphic

		if (!getState().equals(SELECTING_MATERIALS)) {
			List<HaulingAllocation> allocatedItems = getIncomingHaulingAllocations();
			for (QuantifiedItemTypeWithMaterial requirement : getRequirements()) {
				if (requirement.getMaterial() != null) {
					int numberAllocated = getAllocationAmount(requirement.getItemType(), allocatedItems, getPlacedItemAllocations().values(), gameContext);
					description.add(i18nTranslator.getItemAllocationDescription(numberAllocated, requirement)); //TODO: refactor
				}
			}
		}
		return description;
	}


	private int getAllocationAmount(ItemType itemType, List<HaulingAllocation> haulingAllocations, Collection<ItemAllocation> placedItems, GameContext gameContext) {
		int allocated = 0;
		for (HaulingAllocation haulingAllocation : haulingAllocations) {
			if (haulingAllocation.getItemAllocation() != null) {
				Entity itemEntity = gameContext.getEntities().get(haulingAllocation.getItemAllocation().getTargetItemEntityId());
				if (itemEntity != null && ((ItemEntityAttributes)itemEntity.getPhysicalEntityComponent().getAttributes()).getItemType().equals(itemType)) {
					allocated += haulingAllocation.getItemAllocation().getAllocationAmount();
				}
			}
		}
		for (ItemAllocation itemAllocation : placedItems) {
			Entity itemEntity = gameContext.getEntities().get(itemAllocation.getTargetItemEntityId());
			if (itemEntity != null) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(itemType)) {
					allocated += itemAllocation.getAllocationAmount();
				}
			}
		}

		return allocated;
	}
}
