package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.crafting.CraftingOutputQualityDictionary;
import technology.rocketjump.saul.crafting.CraftingRecipeDictionary;
import technology.rocketjump.saul.crafting.model.CraftingOutputQuality;
import technology.rocketjump.saul.crafting.model.CraftingRecipe;
import technology.rocketjump.saul.crafting.model.CraftingRecipeValueConversion;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.saul.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.entities.tags.CraftingOverrideTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.*;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ItemCreationRequestMessage;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;
import technology.rocketjump.saul.messaging.types.RequestLiquidRemovalMessage;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;
import technology.rocketjump.saul.settlement.production.CraftingAssignment;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;

import java.util.*;

import static technology.rocketjump.saul.entities.behaviour.furniture.FillLiquidContainerBehaviour.relatedContainerCapacity;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.saul.entities.model.physical.item.ItemQuality.STANDARD;
import static technology.rocketjump.saul.entities.tags.CraftingOverrideTag.CraftingOverrideSetting.DO_NOT_HAUL_OUTPUT;
import static technology.rocketjump.saul.jobs.model.JobState.ASSIGNABLE;
import static technology.rocketjump.saul.jobs.model.JobState.REMOVED;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.ui.i18n.I18nTranslator.oneDecimalFormat;

public class CraftingStationBehaviour extends FurnitureBehaviour
		implements SelectableDescription,
		Destructible,
		OnJobCompletion,
		Prioritisable,
		ParentDependentEntityComponent {

	public static final float CRAFTING_BONUS_VALUE = 1.6f;
	private CraftingType craftingType;
	private JobType craftItemJobType;
	private JobType haulingJobType;
	private CraftingRecipeDictionary craftingRecipeDictionary;
	private GameMaterialDictionary gameMaterialDictionary;
	private CraftingOutputQualityDictionary craftingOutputQualityDictionary;

	private CraftingAssignment craftingAssignment;
	private boolean requiresExtraTime;
	private Double extraTimeToProcess;
	private Entity jobCompletedByEntity;
	private double lastUpdateGameTime;


	public CraftingStationBehaviour() {

	}

	public CraftingStationBehaviour(CraftingType craftingType, JobType craftItemJobType, JobType haulingJobType,
									GameMaterialDictionary gameMaterialDictionary, CraftingOutputQualityDictionary craftingOutputQualityDictionary,
									CraftingRecipeDictionary craftingRecipeDictionary) {
		this.craftingType = craftingType;
		this.craftItemJobType = craftItemJobType;
		this.haulingJobType = haulingJobType;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.craftingOutputQualityDictionary = craftingOutputQualityDictionary;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		requiresExtraTime = false;
		if (craftingAssignment != null) {
			cancelAssignment();
		}
	}

	@Override
	public CraftingStationBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented clone() in " + this.getClass().getSimpleName());
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		if (parentEntity.isOnFire()) {
			if (craftingAssignment != null) {
				cancelAssignment();
				craftingAssignment = null;
			}
			return;
		}

		if (extraTimeToProcess != null) {
			FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
			if (particleEffectsComponent != null) {
				particleEffectsComponent.triggerProcessingEffects(
						Optional.ofNullable(craftingAssignment != null ? new JobTarget(craftingAssignment.getTargetRecipe(), parentEntity) : null));
			}

			double elapsedTime = gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime;
			extraTimeToProcess -= elapsedTime;
			if (extraTimeToProcess < 0) {
				requiresExtraTime = false;
				extraTimeToProcess = null;
				jobCompleted(gameContext, jobCompletedByEntity);
				jobCompletedByEntity = null;

				InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
				ProductionExportFurnitureBehaviour exportFurnitureBehaviour = gameContext.getAreaMap().getTile(craftingAssignment.getOutputLocation()).getEntities().stream()
						.filter(e -> e.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour)
						.map(e -> (ProductionExportFurnitureBehaviour) e.getBehaviourComponent())
						.findFirst().orElse(null);
				if (exportFurnitureBehaviour != null) {
					InventoryComponent.InventoryEntry outputEntry = inventoryComponent.findByItemTypeAndMaterial(exportFurnitureBehaviour.getSelectedItemType(), exportFurnitureBehaviour.getSelectedMaterial(), gameContext.getGameClock());
					ItemEntityAttributes attributes = (ItemEntityAttributes) outputEntry.entity.getPhysicalEntityComponent().getAttributes();
					int quantityToHaul = Math.min(attributes.getQuantity(), attributes.getItemType().getMaxHauledAtOnce());

					Job haulingJob = new Job(haulingJobType);
					HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(quantityToHaul, outputEntry.entity, parentEntity)
							.toEntity(exportFurnitureBehaviour.getParentEntity());
					haulingJob.setHaulingAllocation(haulingAllocation);
					haulingJob.setRequiredProfession(craftingType.getProfessionRequired());
					haulingJob.setJobPriority(getPriority());
					haulingJob.setJobState(ASSIGNABLE);
					haulingJob.setTargetId(outputEntry.entity.getId());
					haulingJob.setJobLocation(toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition()));
					updateJobLocationIfNotNavigable(haulingJob);
					messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
				}

				assignmentCompleted();

				if (particleEffectsComponent != null) {
					particleEffectsComponent.releaseParticles();
				}
			}
		}

		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();

		ConstructedEntityComponent constructedEntityComponent = parentEntity.getComponent(ConstructedEntityComponent.class);
		if (constructedEntityComponent != null && constructedEntityComponent.isBeingDeconstructed()) {
			this.destroy(parentEntity, messageDispatcher, gameContext);
			return;
		}

		PoweredFurnitureComponent poweredFurnitureComponent = parentEntity.getComponent(PoweredFurnitureComponent.class);
		if (poweredFurnitureComponent != null) {
			poweredFurnitureComponent.update(0, gameContext);
		}

		if (craftingAssignment != null) {

			// TODO check if assignment should be cancelled (e.g. target output changed)

			if (poweredFurnitureComponent != null) {
				adjustPoweredCraftingDuration(gameContext, poweredFurnitureComponent);
			}

			updateJobLocationIfNotNavigable(craftingAssignment.getCraftingJob());

			return; // Waiting for entity to craft item with materials
		}


		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
		if (!inventoryComponent.isEmpty()) {
			clearInventoryItems();
			// Still waiting for items to be collected
			return;
		}

		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
			clearLiquid();
			// Still waiting for liquid to be moved
			return;
		}

		selectCraftingAssignment();
	}

	public boolean allCraftingRequirementsInInventory() {
		if (craftingAssignment == null) {
			return false;
		} else {
			InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
			for (QuantifiedItemTypeWithMaterial inputRequirement : craftingAssignment.getTargetRecipe().getInput()) {
				if (inputRequirement.isLiquid()) {
					// TODO sort out liquids
				} else {
					InventoryComponent.InventoryEntry entry = inventoryComponent.findByItemTypeAndMaterial(inputRequirement.getItemType(), inputRequirement.getMaterial(), gameContext.getGameClock());
					if (entry == null || ((ItemEntityAttributes)entry.entity.getPhysicalEntityComponent().getAttributes()).getQuantity() < inputRequirement.getQuantity()) {
						return false;
					}
				}
			}
			return true;
		}
	}

	private void selectCraftingAssignment() {
		List<CraftingRecipe> recipesForCraftingType = new ArrayList<>(craftingRecipeDictionary.getByCraftingType(craftingType));
		// Randomly sort the recipes so that different recipes with the same output are tried in different updates
		Collections.shuffle(recipesForCraftingType, gameContext.getRandom());

		MapTile mapTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (mapTile.getRoomTile() == null) {
			Logger.error("{} for {} is not in a room", getClass().getSimpleName(), parentEntity);
		}

		List<ProductionImportFurnitureBehaviour> importFurniture = allImportFurniture();
		List<ProductionExportFurnitureBehaviour> exportFurniture = allExportFurniture();

		for (ProductionExportFurnitureBehaviour exportFurnitureBehaviour : exportFurniture) {
			ItemType desiredItemType = exportFurnitureBehaviour.getSelectedItemType();
			if (desiredItemType == null) {
				Logger.warn("Not yet implemented: crafting non-item output");
				continue;
			}
			List<CraftingRecipe> potentialRecipes = recipesForCraftingType.stream()
					.filter(r -> desiredItemType.equals(r.getOutput().getItemType()))
					.toList();
			// need to randomly pick an available receipe or else we might get stuck on one we don't have the requirements for, while ignoring another that we do
			CraftingRecipe matchingRecipe = potentialRecipes.isEmpty() ? null : potentialRecipes.get(gameContext.getRandom().nextInt(potentialRecipes.size()));
			if (matchingRecipe != null) {
				int quantityRequired = getAmountRequired(exportFurnitureBehaviour);
				GameMaterial desiredMaterial = exportFurnitureBehaviour.getSelectedMaterial();
				if (quantityRequired > 0 && quantityRequired >= matchingRecipe.getOutput().getQuantity()) {
					if (matchingRecipe.getInput().stream().allMatch(r -> inputIsAvailable(r, importFurniture, desiredMaterial))) {
						// This recipe can be crafted, so use this and create a CraftingAssignment

						craftingAssignment = new CraftingAssignment(matchingRecipe);

						for (QuantifiedItemTypeWithMaterial inputRequirement : matchingRecipe.getInput()) {
							ProductionImportFurnitureBehaviour matchedInputFurniture = getImportFurnitureForRequirement(inputRequirement, importFurniture, desiredMaterial);
							if (matchedInputFurniture == null) {
								Logger.error("Could not find input allocation for crafting, this should not happen");
								continue;
							}
							addInputToAssignment(inputRequirement, matchedInputFurniture);
						}

						craftingAssignment.setOutputLocation(toGridPoint(exportFurnitureBehaviour.getParentEntity().getLocationComponent().getWorldOrParentPosition()));
						exportFurnitureBehaviour.getPendingAssignments().add(craftingAssignment);

						Job craftingJob = new Job(craftItemJobType);
						craftingAssignment.setCraftingJob(craftingJob);
						craftingJob.setJobPriority(priority);
						craftingJob.setJobState(ASSIGNABLE);
						craftingJob.setTargetId(parentEntity.getId());
						craftingJob.setCraftingRecipe(matchingRecipe);
						ItemType itemTypeRequired = Optional.ofNullable(matchingRecipe.getItemTypeRequired()).orElse(matchingRecipe.getCraftingType().getDefaultItemType());
						craftingJob.setRequiredItemType(itemTypeRequired);
						craftingJob.setRequiredProfession(craftingType.getProfessionRequired());
						craftingJob.setJobLocation(toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition()));
						updateJobLocationIfNotNavigable(craftingAssignment.getCraftingJob());
						messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, craftingJob);

						break;
					}
				}
			}
		}

	}

	private List<ProductionImportFurnitureBehaviour> allImportFurniture() {
		MapTile mapTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (mapTile.getRoomTile() == null) {
			Logger.error("{} for {} is not in a room", getClass().getSimpleName(), parentEntity);
			return List.of();
		}

		return mapTile.getRoomTile().getRoom().getRoomTiles().keySet().stream()
				.map(gameContext.getAreaMap()::getTile)
				.flatMap(t -> t.getEntities().stream())
				.filter(e -> e.getType().equals(EntityType.FURNITURE) && e.getBehaviourComponent() instanceof ProductionImportFurnitureBehaviour)
				.map(furniture -> furniture.getComponent(ProductionImportFurnitureBehaviour.class))
				.filter(e -> e.getSelectedItemType() != null)
				.filter(e -> e.getParentEntity().getComponent(InventoryComponent.class)
						.findByItemTypeAndMaterial(e.getSelectedItemType(), e.getSelectedMaterial(), gameContext.getGameClock()) != null
				)
				.toList();
	}

	private List<ProductionExportFurnitureBehaviour> allExportFurniture() {
		MapTile mapTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (mapTile.getRoomTile() == null) {
			Logger.error("{} for {} is not in a room", getClass().getSimpleName(), parentEntity);
			return List.of();
		}

		List<ProductionExportFurnitureBehaviour> exportFurniture = mapTile.getRoomTile().getRoom().getRoomTiles().keySet().stream()
				.map(gameContext.getAreaMap()::getTile)
				.flatMap(t -> t.getEntities().stream())
				.filter(e -> e.getType().equals(EntityType.FURNITURE) && e.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour)
				.map(furniture -> furniture.getComponent(ProductionExportFurnitureBehaviour.class))
				.filter(e -> e.getSelectedItemType() != null)
				.sorted(Comparator.comparingInt(e -> e.getPriority().ordinal()))
				.toList();
		return exportFurniture;
	}

	private void addInputToAssignment(QuantifiedItemTypeWithMaterial inputRequirement, ProductionImportFurnitureBehaviour matchedInputFurniture) {
		InventoryComponent inputInventory = matchedInputFurniture.getParentEntity().getComponent(InventoryComponent.class);
		Entity inputItemInInventory = inputInventory.findByItemType(inputRequirement.getItemType(), gameContext.getGameClock()).entity;
		ItemAllocationComponent inputAllocationComponent = inputItemInInventory.getComponent(ItemAllocationComponent.class);

		inputAllocationComponent.cancelAll(ItemAllocation.Purpose.PRODUCTION_IMPORT);
		HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(inputRequirement.getQuantity(), inputItemInInventory, parentEntity)
				.toEntity(parentEntity);
		if (inputAllocationComponent.getNumUnallocated() > 0 && inputInventory.getAddAsAllocationPurpose() != null) {
			inputAllocationComponent.createAllocation(inputAllocationComponent.getNumUnallocated(), matchedInputFurniture.getParentEntity(), inputInventory.getAddAsAllocationPurpose());
		}
		craftingAssignment.getInputAllocations().add(haulingAllocation);
	}

	private boolean inputIsAvailable(QuantifiedItemTypeWithMaterial requirement,
									 List<ProductionImportFurnitureBehaviour> importFurniture, GameMaterial desiredMaterial) {
		return getImportFurnitureForRequirement(requirement, importFurniture, desiredMaterial) != null;
	}

	private ProductionImportFurnitureBehaviour getImportFurnitureForRequirement(QuantifiedItemTypeWithMaterial requirement,
																				List<ProductionImportFurnitureBehaviour> importFurniture, GameMaterial desiredMaterial) {
		for (ProductionImportFurnitureBehaviour importBehaviour : importFurniture) {
			if (importBehaviour.getSelectedItemType().equals(requirement.getItemType())) {
				InventoryComponent importInventory = importBehaviour.getParentEntity().getComponent(InventoryComponent.class);
				InventoryComponent.InventoryEntry inventoryEntry = importInventory.findByItemType(requirement.getItemType(), gameContext.getGameClock());
				if (inventoryEntry != null) {
					ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), importBehaviour.getParentEntity(), ItemAllocation.Purpose.PRODUCTION_IMPORT);
					}
					ItemAllocation allocationForImport = itemAllocationComponent
							.getAllocationForPurpose(ItemAllocation.Purpose.PRODUCTION_IMPORT);
					if (allocationForImport != null && allocationForImport.getAllocationAmount() >= requirement.getQuantity()) {
						if (desiredMaterial == null || !requirement.getItemType().getPrimaryMaterialType().equals(desiredMaterial.getMaterialType())) {
							return importBehaviour;
						} else {
							ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
							if (attributes.getPrimaryMaterial().equals(desiredMaterial)) {
								return importBehaviour;
							}
						}
					}
				}

			}
		}
		return null;
	}

	private int getAmountRequired(ProductionExportFurnitureBehaviour exportFurnitureBehaviour) {
		int maxStackSize = exportFurnitureBehaviour.getSelectedItemType().getMaxStackSize();
		InventoryComponent inventoryComponent = exportFurnitureBehaviour.getParentEntity().getComponent(InventoryComponent.class);
		int currentQuantity = 0;
		for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
			currentQuantity += ((ItemEntityAttributes)entry.entity.getPhysicalEntityComponent().getAttributes()).getQuantity();
		}
		int pendingQuantity = exportFurnitureBehaviour.getPendingAssignments().stream()
				.map(a -> a.getTargetRecipe().getOutput().getQuantity())
				.reduce(0, Integer::sum);

		return maxStackSize - currentQuantity - pendingQuantity;
	}

	private void cancelAssignment() {
		// TODO cancel pending inputs, revert pallet item allocations where necessary
		if (!craftingAssignment.getCraftingJob().getJobState().equals(REMOVED)) {
			messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, craftingAssignment.getCraftingJob());
		}
		craftingAssignment = null;
	}

	private void updateJobLocationIfNotNavigable(Job job) {
		boolean jobIsNavigable = gameContext.getAreaMap().getTile(job.getJobLocation()).isNavigable(null);
		if (!jobIsNavigable) {
			FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				job.setJobLocation(navigableWorkspace.getAccessedFrom());
				job.setSecondaryLocation(navigableWorkspace.getLocation());
			}
		}
	}

	private void adjustPoweredCraftingDuration(GameContext gameContext, PoweredFurnitureComponent poweredFurnitureComponent) {
		if (poweredFurnitureComponent.isPowered(poweredFurnitureComponent.getParentUnderTile(gameContext))) {
			// powered, job should take normal length of time
			craftingAssignment.getCraftingJob().setWorkDurationMultiplier(null);
		} else {
			// not powered, make job take longer

			// MODDING It's a bit dirty that the work duration multiplier is using the animation speed, better
			// to separate this out to another variable passed into the PoweredFurnitureComponent
			craftingAssignment.getCraftingJob().setWorkDurationMultiplier(poweredFurnitureComponent.getAnimationSpeed());
		}
	}

	public boolean needsExtraTimeToProcess() {
		return extraTimeToProcess != null;
	}

	@Override
	public void jobCompleted(GameContext gameContext, Entity completedByEntity) {
		if (requiresExtraTime) {
			extraTimeToProcess = this.craftingAssignment.getTargetRecipe().getExtraGameHoursToComplete();
			jobCompletedByEntity = completedByEntity;
			return;
		}

		// Replace input in inventory with output
		List<Entity> output = new ArrayList<>();
		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);

		if (craftingAssignment == null || craftingAssignment.getTargetRecipe() == null) {
			// FIXME Not seen this happen but has come up in a bug report
			Logger.error("Null production assignment in jobCompleted() in " + this.getClass().getSimpleName());
		} else {
			QuantifiedItemTypeWithMaterial outputRequirement = craftingAssignment.getTargetRecipe().getOutput();
			if (outputRequirement.isLiquid()) {
				// Just switching contents of liquid container, no material transfer possible? Implies liquids should be more than a material
				// I.e. current materials (water, wort) are equivalent to ItemType (LiquidType?) which have one or more materials, same for soup
				liquidContainerComponent.cancelAllAllocations();
				liquidContainerComponent.setLiquidQuantity(outputRequirement.getQuantity());
				liquidContainerComponent.setTargetLiquidMaterial(outputRequirement.getMaterial());
			} else {
				addOutputItemToList(outputRequirement, gameContext, output, completedByEntity);
			}
		}

		if (liquidContainerComponent != null &&
				!craftingAssignment.getTargetRecipe().getOutput().isLiquid()) {
			// No liquid outputs but liquid container, should clear
			liquidContainerComponent.cancelAllAllocations();
			liquidContainerComponent.setLiquidQuantity(0);
			liquidContainerComponent.setTargetLiquidMaterial(null);
		}

		int inputItemsTotalValue = calculateTotalInputValue(inventoryComponent.getInventoryEntries());

		for (InventoryComponent.InventoryEntry inventoryEntry : new ArrayList<>(inventoryComponent.getInventoryEntries())) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.entity);
		}
		for (Entity outputEntity : output) {
			inventoryComponent.add(outputEntity, parentEntity, messageDispatcher, gameContext.getGameClock());
			// Unallocate from inventory
			outputEntity.getOrCreateComponent(ItemAllocationComponent.class).cancelAll(HELD_IN_INVENTORY);

			setItemValue(inputItemsTotalValue / output.size(), outputEntity, craftingAssignment.getTargetRecipe().getValueConversion());
		}

//		this.craftingAssignment = null;
		this.requiresExtraTime = false;

		// rerun update to trigger export item/liquid jobs
		infrequentUpdate(gameContext);
	}

	private void setItemValue(int inputItemsValue, Entity outputEntity, CraftingRecipeValueConversion valueConversion) {
		switch (valueConversion) {
			case DEFAULT -> {
				ItemEntityAttributes outputAttributes = (ItemEntityAttributes) outputEntity.getPhysicalEntityComponent().getAttributes();
				float totalValue = (float) inputItemsValue * CRAFTING_BONUS_VALUE * outputAttributes.getItemQuality().valueMultiplier;
				int valuePerItem = Math.max(1, Math.round(totalValue / (float) outputAttributes.getQuantity()));
				outputAttributes.setValuePerItem(valuePerItem);
			}
			case USE_OUTPUT_BASE_VALUE -> {
				// Do nothing, value should have been set when item was created
			}
			default -> Logger.error("Not yet implemented: " + valueConversion);
		}
	}

	private int calculateTotalInputValue(Collection<InventoryComponent.InventoryEntry> inventoryEntries) {
		return inventoryEntries.stream()
				.map(e -> e.entity)
				.filter(e -> e.getType().equals(EntityType.ITEM))
				.map(e -> ((ItemEntityAttributes) e.getPhysicalEntityComponent().getAttributes()).getTotalValue())
				.reduce(0, Integer::sum);
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		if (craftingAssignment != null) {
			craftingAssignment.getCraftingJob().setJobPriority(priority);
		}
	}

	private void addOutputItemToList(QuantifiedItemTypeWithMaterial outputRequirement, GameContext gameContext, List<Entity> output, Entity completedByEntity) {
		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		ItemEntityAttributes outputAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
		outputAttributes.setItemType(outputRequirement.getItemType());
		outputAttributes.setItemPlacement(ItemPlacement.ON_GROUND);
		if (!outputAttributes.getItemType().isStackable() && completedByEntity != null) {
			outputAttributes.setItemQuality(determineItemQuality(completedByEntity, gameContext.getRandom()));
		}


		if (craftingAssignment.getTargetRecipe().getMaterialTypesToCopyOver() != null) {
			for (GameMaterialType materialTypeToCopy : craftingAssignment.getTargetRecipe().getMaterialTypesToCopyOver()) {
				GameMaterial materialToAdd = null;
				for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
					ItemEntityAttributes inventoryAttributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
					materialToAdd = inventoryAttributes.getMaterial(materialTypeToCopy);
					if (materialToAdd != null) {
						break;
					}
				}
				if (materialToAdd != null) {
					outputAttributes.setMaterial(materialToAdd);
				}
			}
		}

		// Force any specified output materials
		if (outputRequirement.getMaterial() != null) {
			outputAttributes.setMaterial(outputRequirement.getMaterial());
		}

		// Randomly add any missing material types
		for (GameMaterialType requiredMaterialType : outputAttributes.getItemType().getMaterialTypes()) {
			if (outputAttributes.getMaterial(requiredMaterialType) == null) {
				List<GameMaterial> materialsToPickFrom;

				if (requiredMaterialType.equals(outputAttributes.getItemType().getPrimaryMaterialType()) &&
						!outputAttributes.getItemType().getSpecificMaterials().isEmpty()) {
					materialsToPickFrom = outputAttributes.getItemType().getSpecificMaterials();
				} else {
					materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
							.filter(GameMaterial::isUseInRandomGeneration).toList();
				}
				GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
				outputAttributes.setMaterial(material);
			}
		}

		outputAttributes.setQuantity(outputRequirement.getQuantity());

		messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(outputAttributes, (outputItem) -> {
			output.add(outputItem);
		}));
	}

	private ItemQuality determineItemQuality(Entity completedByEntity, Random random) {
		ItemQuality result = STANDARD;
		if (craftingType.getProfessionRequired() != null) {
			SkillsComponent skillsComponent = completedByEntity.getComponent(SkillsComponent.class);
			if (skillsComponent != null) {
				int skillLevel = skillsComponent.getSkillLevel(craftingType.getProfessionRequired());
				CraftingOutputQuality outputQuality = craftingOutputQualityDictionary.getForSkillLevel(skillLevel);

				float roll = random.nextFloat();
				for (Map.Entry<ItemQuality, Float> entry : outputQuality.getOutputQuality().entrySet()) {
					result = entry.getKey();
					roll -= entry.getValue();
					if (roll <= 0) {
						break;
					}
				}
			}
		}
		return result;
	}

	private boolean outputHaulingAllowed() {
		// TODO think we can drop this flag/tag?
		CraftingOverrideTag craftingOverrideTag = parentEntity.getTag(CraftingOverrideTag.class);
		return craftingOverrideTag == null || !craftingOverrideTag.includes(DO_NOT_HAUL_OUTPUT);
	}

	private void clearInventoryItems() {
		if (!outputHaulingAllowed()) {
			return;
		}

		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
			ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
			itemAllocationComponent.cancelAll(HELD_IN_INVENTORY);
			if (itemAllocationComponent.getNumUnallocated() > 0) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(entry.entity, parentEntity, true, priority, job -> {
					// Do nothing with job
					if (job != null && job.getHaulingAllocation() != null) {
						HaulingAllocation allocation = job.getHaulingAllocation();
						allocation.setTargetId(null);
						allocation.setTargetPositionType(null);
						allocation.setTargetPosition(null);
					}
				}));
			}
		}

	}

	private void clearLiquid() {
		if (!outputHaulingAllowed()) {
			return;
		}

		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent.getNumUnallocated() > 0) {
			float amount = Math.min(liquidContainerComponent.getNumUnallocated(), relatedContainerCapacity(relatedItemTypes.get(0)));
			FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_REMOVAL, new RequestLiquidRemovalMessage(parentEntity,
						navigableWorkspace.getAccessedFrom(), amount, relatedItemTypes.get(0), priority, job -> {
					if (job != null) {
//						liquidTransferJobs.add(job);
					}
				}));
			}
		}
	}

	public CraftingAssignment getCurrentCraftingAssignment() {
		return craftingAssignment;
	}

	@Override
	public void update(float deltaTime) {
		// Do nothing every frame
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		List<I18nText> descriptions = new ArrayList<>(2);

		if (craftingAssignment != null) {
			I18nText targetDescription;
			QuantifiedItemTypeWithMaterial output = craftingAssignment.getTargetRecipe().getOutput();
			if (output.isLiquid()) {
				targetDescription = i18nTranslator.getLiquidDescription(output.getMaterial(), output.getQuantity());
			} else {
				targetDescription = i18nTranslator.getItemDescription(
						output.getQuantity(),
						output.getMaterial(),
						output.getItemType(), null);
			}
			descriptions.add(i18nTranslator.getTranslatedWordWithReplacements("ACTION.JOB.CREATE_GENERIC",
					ImmutableMap.of("targetDescription", targetDescription)));
		}
		if (requiresExtraTime) {
			Double totalExtraHours = craftingAssignment.getTargetRecipe().getExtraGameHoursToComplete();
			double progress = (totalExtraHours - (extraTimeToProcess == null ? totalExtraHours : extraTimeToProcess)) / totalExtraHours;
			descriptions.add(i18nTranslator.getTranslatedWordWithReplacements("FURNITURE.DESCRIPTION.GENERIC_PROGRESS",
					ImmutableMap.of("progress", new I18nWord(oneDecimalFormat.format(100f * progress)))));
		}
		return descriptions;
	}

	public CraftingType getCraftingType() {
		return this.craftingType;
	}

	public void assignmentCompleted() {
		if (extraTimeToProcess != null)	{
			// still waiting for time to elapse, so don't mark as completed yet
		} else {
			craftingAssignment = null;
		}
		infrequentUpdate(gameContext);
	}

	public void allocationCancelled(HaulingAllocation allocation) {
		if (craftingAssignment == null) {
			return;
		}
		// an input allocation was cancelled, need to re-assign or else abandon the crafting assignment

		Entity itemEntity = gameContext.getEntities().get(allocation.getHauledEntityId());
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		ItemType itemTypeRequired = attributes.getItemType();
		int amountRequired = allocation.getItemAllocation().getAllocationAmount();

		Entity targetExportFurniture = gameContext.getAreaMap().getTile(craftingAssignment.getOutputLocation())
				.getEntities().stream().filter(e -> e.getType().equals(EntityType.FURNITURE) && e.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour)
				.findAny().orElse(null);

		if (targetExportFurniture == null) {
			cancelAssignment();
		} else {
			GameMaterial desiredMaterial = ((ProductionExportFurnitureBehaviour)targetExportFurniture.getBehaviourComponent()).getSelectedMaterial();

			QuantifiedItemTypeWithMaterial matchedRequirement = craftingAssignment.getTargetRecipe().getInput().stream()
					.filter(q -> q.getItemType().equals(itemTypeRequired) && q.getQuantity() == amountRequired)
					.findAny().orElse(null);

			if (matchedRequirement == null) {
				Logger.error("Could not match up input requirement for cancelled hauling allocation");
				cancelAssignment();
			} else {
				ProductionImportFurnitureBehaviour newInputFurniture = getImportFurnitureForRequirement(matchedRequirement, allImportFurniture(), desiredMaterial);
				if (newInputFurniture == null) {
					cancelAssignment();
				} else {
					addInputToAssignment(matchedRequirement, newInputFurniture);
				}
			}
		}
	}

	public void liquidAdded(float quantity, TiledMap areaMap) {
		// TODO, still needed?
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("craftingType", craftingType.getName());
		asJson.put("craftItemJobType", craftItemJobType.getName());
		asJson.put("haulingJobType", haulingJobType.getName());
		if (requiresExtraTime) {
			asJson.put("requiresExtraTime", true);
		}
		if (extraTimeToProcess != null) {
			asJson.put("extraTimeToProcess", extraTimeToProcess);
		}
		if (jobCompletedByEntity != null) {
			jobCompletedByEntity.writeTo(savedGameStateHolder);
			asJson.put("jobCompletedByEntity", jobCompletedByEntity.getId());
		}

		if (craftingAssignment != null) {
			craftingAssignment.writeTo(savedGameStateHolder);
			asJson.put("craftingAssignment", craftingAssignment.getCraftingAssignmentId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.craftingType = relatedStores.craftingTypeDictionary.getByName(asJson.getString("craftingType"));
		if (this.craftingType == null) {
			throw new InvalidSaveException("Could not find crafting type by name " + asJson.getString("craftingType"));
		}
		this.craftItemJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("craftItemJobType"));
		if (craftItemJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("craftItemJobType"));
		}
		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
		this.requiresExtraTime = asJson.getBooleanValue("requiresExtraTime");
		this.extraTimeToProcess = asJson.getDouble("extraTimeToProcess");
		Long jobCompletedByEntityId = asJson.getLong("jobCompletedByEntity");
		if (jobCompletedByEntityId != null) {
			jobCompletedByEntity = savedGameStateHolder.entities.get(jobCompletedByEntityId);
			if (jobCompletedByEntity == null) {
				throw new InvalidSaveException("Could not find entity with ID " + jobCompletedByEntityId + " for jobCompletedByEntity in " + getClass().getSimpleName());
			}
		}

		this.gameMaterialDictionary = relatedStores.gameMaterialDictionary;
		this.craftingOutputQualityDictionary = relatedStores.craftingOutputQualityDictionary;
		this.craftingRecipeDictionary = relatedStores.craftingRecipeDictionary;

		Long craftingAssignmentId = asJson.getLong("craftingAssignment");
		if (craftingAssignmentId != null) {
			craftingAssignment = savedGameStateHolder.craftingAssignments.get(craftingAssignmentId);
			if (craftingAssignment == null) {
				throw new InvalidSaveException("Could not find crafting assignment with ID " + craftingAssignmentId + " for " + getClass().getSimpleName());
			}
		}

	}
}
