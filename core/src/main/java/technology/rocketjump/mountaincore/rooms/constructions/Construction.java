package technology.rocketjump.mountaincore.rooms.constructions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.tags.ConstructionOverrideTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemMaterialSelectionMessage;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.Persistable;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWord;

import java.util.*;

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
	private List<Job> incomingHaulingJobs = new ArrayList<>();
	protected Map<GridPoint2, ItemAllocation> placedItemAllocations = new HashMap<>();
	protected GameMaterialType primaryMaterialType;
	protected List<QuantifiedItemTypeWithMaterial> requirements = new ArrayList<>(); // Note that material will be null initially
	protected List<ItemTypeWithMaterial> playerRequirementSelections = new ArrayList<>();
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
		for (Job incomingHaulingJob : incomingHaulingJobs) {
			incomingHaulingJob.setJobPriority(priority);
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

				if (constructionOverrideSettings.contains(ConstructionOverrideTag.ConstructionOverrideSetting.REQUIRES_EDIBLE_LIQUID)) {
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

		ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(attributes.getQuantity(), itemEntity, ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION);
		this.placedItemAllocations.put(haulingAllocation.getTargetPosition(), itemAllocation);
	}

	public void placedItemQuantityIncreased(HaulingAllocation haulingAllocation, Entity existingItem) {
		this.allocationCancelled(haulingAllocation);

		ItemAllocationComponent itemAllocationComponent = existingItem.getOrCreateComponent(ItemAllocationComponent.class);
		ItemEntityAttributes attributes = (ItemEntityAttributes) existingItem.getPhysicalEntityComponent().getAttributes();

		ItemAllocation existingItemAllocation = itemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION);
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

	public void addHaulingJob(Job haulingJob) {
		this.incomingHaulingJobs.add(haulingJob);
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
		Entity entity = getEntity();
		if (entity != null) {
			//materials assigned elsewhere like SpecificExtraMaterialsTag
			if (entity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes furnitureEntityAttributes
			&& furnitureEntityAttributes.getPrimaryMaterial() != null) {

				return furnitureEntityAttributes.getPrimaryMaterial();
			}
		}
		return GameMaterial.NULL_MATERIAL;
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

	public List<ItemTypeWithMaterial> getPlayerRequirementSelections() {
		return playerRequirementSelections;
	}

	public void setPlayerRequirementSelections(List<ItemTypeWithMaterial> playerRequirementSelections) {
		this.playerRequirementSelections.clear();
		this.playerRequirementSelections.addAll(playerRequirementSelections);
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

		if (!incomingHaulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : incomingHaulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
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

		if (!playerRequirementSelections.isEmpty()) {
			JSONArray playerRequirementJson = new JSONArray();
			for (ItemTypeWithMaterial playerRequirementSelection : playerRequirementSelections) {
				JSONObject requirementJson = new JSONObject(true);
				playerRequirementSelection.writeTo(requirementJson, savedGameStateHolder);
				playerRequirementJson.add(requirementJson);
			}
			asJson.put("playerRequirementSelections", playerRequirementJson);
		}

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

		JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
		if (incomingHaulingJobsJson != null) {
			for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
				long jobId = incomingHaulingJobsJson.getLongValue(cursor);
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				} else {
					incomingHaulingJobs.add(job);
				}
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

		JSONArray playerRequirementsJson = asJson.getJSONArray("playerRequirementSelections");
		if (playerRequirementsJson != null) {
			for (int cursor = 0; cursor < playerRequirementsJson.size(); cursor++) {
				ItemTypeWithMaterial requirement = new ItemTypeWithMaterial();
				requirement.readFrom(playerRequirementsJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
				this.playerRequirementSelections.add(requirement);
			}
		}

		savedGameStateHolder.constructions.put(getId(), this);
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		List<I18nText> description = new ArrayList<>(2 + requirements.size());

		description.add(getHeadlineDescription(i18nTranslator));
		description.addAll(getConstructionStatusDescriptions(i18nTranslator, messageDispatcher));

		if (!getState().equals(ConstructionState.SELECTING_MATERIALS)) {
			for (QuantifiedItemTypeWithMaterial requirement : getRequirements()) {
				if (requirement.getMaterial() != null) {
					int numberAllocated = getAllocationAmount(requirement.getItemType(), gameContext);
					description.add(getItemAllocationDescription(numberAllocated, requirement, i18nTranslator));
				}
			}
		}
		return description;
	}

	public abstract String getFurnitureTypeI18nKey();

	public I18nText getHeadlineDescription(I18nTranslator translator) {
		GameMaterial primaryMaterial = getPrimaryMaterial();
		Map<String, I18nString> replacements = new HashMap<>();
		if (GameMaterial.NULL_MATERIAL.equals(primaryMaterial)) {
			replacements.put("materialType", I18nWord.BLANK);
		} else {
			replacements.put("materialType", primaryMaterial.getI18nValue());
		}
		replacements.put("furnitureType", translator.getWord(getFurnitureTypeI18nKey()));

		return translator.applyReplacements(translator.getWord("CONSTRUCTION.DESCRIPTION"), replacements, Gender.ANY);

//		} else if (construction instanceof BridgeConstruction) {
//			return getConstructionDescription(getPrimaryMaterial(), ((BridgeConstruction)construction).getBridge().getBridgeType().getI18nKey());
//		}
	}

	public int getAllocationAmount(ItemType itemType, GameContext gameContext) {
		int allocated = 0;
		for (HaulingAllocation haulingAllocation : getIncomingHaulingAllocations()) {
			if (haulingAllocation.getItemAllocation() != null) {
				Entity itemEntity = gameContext.getEntities().get(haulingAllocation.getItemAllocation().getTargetItemEntityId());
				if (itemEntity != null && ((ItemEntityAttributes)itemEntity.getPhysicalEntityComponent().getAttributes()).getItemType().equals(itemType)) {
					allocated += haulingAllocation.getItemAllocation().getAllocationAmount();
				}
			}
		}
		for (ItemAllocation itemAllocation : getPlacedItemAllocations().values()) {
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

	public List<I18nText> getConstructionStatusDescriptions(I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher) {
		List<I18nText> descriptions = new ArrayList<>();

		if (ConstructionState.SELECTING_MATERIALS == getState()) {
			for (QuantifiedItemTypeWithMaterial requirement : getRequirements()) {
				if (requirement.getMaterial() == null) {

					messageDispatcher.dispatchMessage(MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE, new ItemMaterialSelectionMessage(
							requirement.getItemType(),
							requirement.getQuantity(),
							foundMaterial -> {
								if (foundMaterial == null) {
									Map<String, I18nString> replacements = new HashMap<>();
									I18nWord word = i18nTranslator.getWord("CONSTRUCTION.STATUS.SELECTING_MATERIALS");
									ItemType missingItemType = requirement.getItemType();

									if (missingItemType != null) {
										replacements.put("materialType", i18nTranslator.getWord(missingItemType.getPrimaryMaterialType().getI18nKey()));
										replacements.put("itemDescription", i18nTranslator.getWord(missingItemType.getI18nKey()));
									}

									descriptions.add(i18nTranslator.applyReplacements(word, replacements, Gender.ANY));
								}
							}
					));


				}
			}
		} else {
			Map<String, I18nString> replacements = new HashMap<>();
			I18nWord word;
			switch (getState()) {
				case CLEARING_WORK_SITE:
					word = i18nTranslator.getWord("CONSTRUCTION.STATUS.CLEARING_WORK_SITE");
					break;
				case WAITING_FOR_RESOURCES:
					word = i18nTranslator.getWord("CONSTRUCTION.STATUS.WAITING_FOR_RESOURCES");
					break;
				case WAITING_FOR_COMPLETION:
					word = i18nTranslator.getWord("CONSTRUCTION.STATUS.WAITING_FOR_COMPLETION");
					break;
				default:
					Logger.error("Not yet implemented: Construction state description for " + getState());
					return List.of(I18nText.BLANK);
			}

			descriptions.add(i18nTranslator.applyReplacements(word, replacements, Gender.ANY));
		}

		return descriptions;
	}

	public I18nText getItemAllocationDescription(int numberAllocated, QuantifiedItemTypeWithMaterial requirement, I18nTranslator i18nTranslator) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("quantity", new I18nWord(String.valueOf(numberAllocated)));
		replacements.put("total", new I18nWord(String.valueOf(requirement.getQuantity())));
		replacements.put("itemDescription", i18nTranslator.getItemDescription(requirement.getQuantity(), requirement.getMaterial(), requirement.getItemType(), null));

		return i18nTranslator.applyReplacements(i18nTranslator.getWord("CONSTRUCTION.ITEM_ALLOCATION"), replacements, Gender.ANY);
	}

}
