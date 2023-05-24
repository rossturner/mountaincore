package technology.rocketjump.mountaincore.rooms.components.behaviour;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.cooking.model.CookingRecipe;
import technology.rocketjump.mountaincore.cooking.model.CookingSession;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CollectItemFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.JobCompletedMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestLiquidTransferMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.components.RoomComponent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static technology.rocketjump.mountaincore.jobs.model.JobState.REMOVED;
import static technology.rocketjump.mountaincore.misc.VectorUtils.toGridPoint;

public class KitchenBehaviour extends RoomBehaviourComponent implements Telegraph, Prioritisable {

	private Collection<CookingRecipe> cookingRecipes;

	private Map<Long, CookingSession> cookingSessions = new HashMap<>();
	private Map<Long, Entity> furnitureEntities = new HashMap<>();
	private Skill requiredProfession;
	private JobType cookingJobType;
	private JobType transferLiquidJobType;
	private JobType haulingJobType;

	public KitchenBehaviour(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
		messageDispatcher.addListener(this, MessageType.JOB_CANCELLED);
		messageDispatcher.addListener(this, MessageType.JOB_COMPLETED);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		messageDispatcher.removeListener(this, MessageType.JOB_CANCELLED);
		messageDispatcher.removeListener(this, MessageType.JOB_COMPLETED);
	}

	@Override
	public RoomComponent clone(Room newParent) {
		KitchenBehaviour cloned = new KitchenBehaviour(newParent, this.messageDispatcher);
		cloned.cookingRecipes = this.cookingRecipes;
		cloned.cookingSessions = new HashMap<>(this.cookingSessions);
		cloned.requiredProfession = this.requiredProfession;
		cloned.cookingJobType = this.cookingJobType;
		cloned.transferLiquidJobType = this.transferLiquidJobType;
		cloned.haulingJobType = this.haulingJobType;
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		KitchenBehaviour other = (KitchenBehaviour) otherComponent;
		// TODO merge any state info together
		this.cookingSessions.putAll(other.cookingSessions);
	}


	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (CookingSession cookingSession : cookingSessions.values()) {
			for (Job job : cookingSession.getInputIngredientJobs()) {
				job.setJobPriority(jobPriority);
			}
			if (cookingSession.getCookingJob() != null) {
				cookingSession.getCookingJob().setJobPriority(jobPriority);
			}
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		refreshFurnitureEntities(gameContext);
		double gameTime = gameContext.getGameClock().getCurrentGameTime();

		Map<FurnitureType, List<CookingRecipe>> byFurnitureType = new HashMap<>();
		for (CookingRecipe recipe : cookingRecipes) {
			if (!byFurnitureType.containsKey(recipe.getCookedInFurniture())) {
				byFurnitureType.put(recipe.getCookedInFurniture(), new ArrayList<>());
			}
			byFurnitureType.get(recipe.getCookedInFurniture()).add(recipe);
		}

		for (Entity entity : furnitureEntities.values()) {
			boolean isNotCooking = !cookingSessions.containsKey(entity.getId());

			if (isNotCooking) {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				List<CookingRecipe> availableRecipes = byFurnitureType.get(attributes.getFurnitureType());
				if (availableRecipes != null) {
					Collections.shuffle(availableRecipes, gameContext.getRandom());
					for (CookingRecipe cookingRecipe : availableRecipes) {
						if (ingredientsAreAvailable(cookingRecipe)) {
							CookingSession cookingSession = new CookingSession(cookingRecipe, entity, gameTime);
							cookingSessions.put(entity.getId(), cookingSession);
							break;
						}
					}
				}
			}
		}
		cookingSessions.entrySet().removeIf(entry -> {
			CookingSession cookingSession = entry.getValue();
			if (!cookingSession.isCompleted() && Math.abs(gameTime - cookingSession.getGameTimeStart()) > 12) {


				LiquidContainerComponent liquid = cookingSession.getAssignedFurnitureEntity().getComponent(LiquidContainerComponent.class);
				InventoryComponent inventory = cookingSession.getAssignedFurnitureEntity().getComponent(InventoryComponent.class);
				float ingredientCount = 0;
				if (liquid != null) {
					ingredientCount += liquid.getLiquidQuantity();
				}
				if (inventory != null) {
					ingredientCount += inventory.getInventoryEntries().size();
				}
				ingredientCount += cookingSession.getInputIngredientJobs().size();
				return ingredientCount == 0;
			}
			return false;
		});


		for (CookingSession cookingSession : cookingSessions.values()) {
			if (!cookingSession.isCompleted()) {
				addAnyMissingJobs(cookingSession, gameContext);

				if (cookingSession.getCookingJob() == null && allIngredientsInPlace(cookingSession)) {
					addCookingJob(cookingSession);
				}
			}
		}
	}

	private boolean ingredientsAreAvailable(CookingRecipe cookingRecipe) {
		AtomicInteger numRequired = new AtomicInteger(cookingRecipe.getInputItemQuantity());
		for (ItemTypeWithMaterial inputItemOption : cookingRecipe.getInputItemOptions()) {
			messageDispatcher.dispatchMessage(MessageType.CHECK_ITEM_AVAILABILITY, new MessageType.CheckItemAvailabilityMessage(inputItemOption, (available) -> {
				numRequired.set(numRequired.get() - available);
			}));
			if (numRequired.get() <= 0) {
				return true;
			}
		}
		return false;
	}

	private void refreshFurnitureEntities(GameContext gameContext) {
		furnitureEntities.clear();
		for (RoomTile roomTile : parent.getRoomTiles().values()) {
			for (Entity entity : roomTile.getTile().getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					furnitureEntities.put(entity.getId(), entity);
				}
			}
		}


		for (CookingSession cookingSession : new ArrayList<>(cookingSessions.values())) {
			if (!furnitureEntities.containsKey(cookingSession.getAssignedFurnitureEntity().getId())) {
				// furniture entity no longer in room
				cookingSessions.remove(cookingSession.getAssignedFurnitureEntity().getId());
			} else if (cookingSession.isCompleted()) {
				Entity assignedFurniture = furnitureEntities.get(cookingSession.getAssignedFurnitureEntity().getId());
				InventoryComponent inventoryComponent = assignedFurniture.getComponent(InventoryComponent.class);
				LiquidContainerComponent liquidContainerComponent = assignedFurniture.getComponent(LiquidContainerComponent.class);
				if ((inventoryComponent == null || inventoryComponent.isEmpty()) &&
						(liquidContainerComponent == null || liquidContainerComponent.isEmpty())) {
					cookingSessions.remove(cookingSession.getAssignedFurnitureEntity().getId());
				}
			}
		}
	}


	private void addAnyMissingJobs(CookingSession cookingSession, GameContext gameContext) {
		cookingSession.getInputIngredientJobs().removeIf(job -> job.getJobState().equals(REMOVED));

		if (cookingSession.getRecipe().getInputLiquidQuantity() > 0) {
			// Requires liquid
			addLiquidTransferJobIfRequired(cookingSession, gameContext);
		}

		if (cookingSession.getRecipe().getInputItemQuantity() > 0) {
			// Requires items
			addHaulIngredientJobIfRequired(cookingSession, gameContext);
		}
	}

	private void addLiquidTransferJobIfRequired(CookingSession cookingSession, GameContext gameContext) {
		LiquidContainerComponent liquidContainerComponent = cookingSession.getAssignedFurnitureEntity().getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent == null) {
			Logger.error("Furniture () assigned with recipe " + cookingSession.getRecipe().getRecipeName() + " is not a liquid container despite requiring liquid input");
		} else {
			float currentAmount = liquidContainerComponent.getLiquidQuantity();
			for (Job job : cookingSession.getInputIngredientJobs()) {
				if (job.getType().equals(transferLiquidJobType)) {
					// FIXME need better way of interrogating item type for liquid capacity
					if (job.getHaulingAllocation() != null && job.getHaulingAllocation().getItemAllocation() != null) {
						Entity hauledItem = gameContext.getEntities().get(job.getHaulingAllocation().getItemAllocation().getTargetItemEntityId());
						if (hauledItem != null) {
							ItemEntityAttributes attributes = (ItemEntityAttributes) hauledItem.getPhysicalEntityComponent().getAttributes();
							List<String> args = attributes.getItemType().getTags().get("LIQUID_CONTAINER");
							if (args != null && !args.isEmpty()) {
								currentAmount += Float.valueOf(args.get(0));
							} else {
								Logger.error("Something went wrong while trying to calculate item type liquid capacity");
								currentAmount += 1f;
							}
						} else {
							Logger.warn("Hauled item from cooking session input transferLiquidJob is null, investigate why");
						}
					} else {
						currentAmount++;
					}
				}
			}

			if (currentAmount < cookingSession.getRecipe().getInputLiquidQuantity()) {
				// Need to create a transfer liquid job
				for (ItemTypeWithMaterial inputLiquidOption : cookingSession.getRecipe().getInputLiquidOptions()) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_TRANSFER, new RequestLiquidTransferMessage(
							inputLiquidOption.getMaterial(), true, cookingSession.getAssignedFurnitureEntity(),
							cookingSession.getAssignedFurnitureEntity().getLocationComponent().getWorldPosition(), inputLiquidOption.getItemType(),
							requiredProfession, priority, job ->
					{
						if (job != null) {
							cookingSession.getInputIngredientJobs().add(job);
						}
					}));
				}
			}
		}
	}

	private void addHaulIngredientJobIfRequired(CookingSession cookingSession, GameContext gameContext) {
		InventoryComponent inventoryComponent = cookingSession.getAssignedFurnitureEntity().getOrCreateComponent(InventoryComponent.class);
		int totalItems = 0;
		for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
			if (matches(inventoryEntry.entity, cookingSession.getRecipe().getInputItemOptions())) {
				totalItems += ((ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes()).getQuantity();
			}
		}

		// Add any incoming hauling jobs
		for (Job job : cookingSession.getInputIngredientJobs()) {
			if (job.getType().equals(haulingJobType)) {
				totalItems += job.getHaulingAllocation().getItemAllocation().getAllocationAmount();
			}
		}

		int amountRequired = cookingSession.getRecipe().getInputItemQuantity() - totalItems;
		if (totalItems < cookingSession.getRecipe().getInputItemQuantity()) {
			// Need to add another ingredient
			addHaulIngredientJob(cookingSession, gameContext, amountRequired);
		}
	}

	private void addHaulIngredientJob(CookingSession cookingSession, GameContext gameContext, int amountRequired) {
		// Look in inventories of furniture entities with COOKING_INGREDIENTS type of COLLECT_ITEMS_BEHAVIOUR tag
		List<Entity> ingredientContainerEntities = new ArrayList<>();
		for (Entity entity : furnitureEntities.values()) {
			BehaviourComponent furnitureBehaviour = entity.getBehaviourComponent();
			if (furnitureBehaviour != null && furnitureBehaviour instanceof CollectItemFurnitureBehaviour) {
				ingredientContainerEntities.add(entity);
			}
		}

		Collections.shuffle(ingredientContainerEntities, gameContext.getRandom());

		Entity foundIngredient = null;
		for (Entity ingredientContainerEntity : ingredientContainerEntities) {
			InventoryComponent inventoryComponent = ingredientContainerEntity.getOrCreateComponent(InventoryComponent.class);
			Collection<InventoryComponent.InventoryEntry> inventoryEntries = inventoryComponent.getInventoryEntries();
			List<InventoryComponent.InventoryEntry> shuffledEntries = new ArrayList<>(inventoryEntries);
			Collections.shuffle(shuffledEntries, gameContext.getRandom());
			Entity matchedItem = getMatch(shuffledEntries, cookingSession.getRecipe().getInputItemOptions());
			if (matchedItem != null) {
				foundIngredient = matchedItem;
				break;
			}
		}

		if (foundIngredient != null) {
			// Always only move 1 ingredient at a time so we can get a good mix of inputs // TODO make this conditional on recipe
			HaulingAllocation ingredientHaulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(1, foundIngredient, cookingSession.getAssignedFurnitureEntity())
							.toEntity(cookingSession.getAssignedFurnitureEntity());

			createHaulingJob(cookingSession, gameContext, foundIngredient, ingredientHaulingAllocation);
		} else {
			// Try to requestAllocation from elsewhere
			ItemTypeWithMaterial requirement = cookingSession.getRecipe().getInputItemOptions().get(gameContext.getRandom().nextInt(cookingSession.getRecipe().getInputItemOptions().size()));
			messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
					cookingSession.getAssignedFurnitureEntity(),
					cookingSession.getAssignedFurnitureEntity().getLocationComponent().getWorldOrParentPosition(), requirement.getItemType(), requirement.getMaterial(), true, amountRequired, null, ingredientHaulingAllocation -> {
						if (ingredientHaulingAllocation != null) {
							Entity foundIngredientEntity = gameContext.getEntities().get(ingredientHaulingAllocation.getHauledEntityId());

							createHaulingJob(cookingSession, gameContext, foundIngredientEntity, ingredientHaulingAllocation);
						}
			}));
		}

	}

	private void createHaulingJob(CookingSession cookingSession, GameContext gameContext, Entity foundIngredient, HaulingAllocation haulingAllocation) {
		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(priority);
		haulingJob.setRequiredProfession(requiredProfession);
		haulingJob.setTargetId(foundIngredient.getId());
		haulingJob.setHaulingAllocation(haulingAllocation);

		if (foundIngredient.getLocationComponent().getContainerEntity() != null) {
			FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(foundIngredient.getLocationComponent().getContainerEntity(), gameContext.getAreaMap());
			if (navigableWorkspace == null) {
				Logger.error("Could not find navigable workspace to create hauling job from");
			} else {
				haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
			}
		} else {
			haulingJob.setJobLocation(toGridPoint(foundIngredient.getLocationComponent().getWorldPosition()));
		}

		if (haulingJob.getJobLocation() != null) {
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
			cookingSession.getInputIngredientJobs().add(haulingJob);
		} else {
			// Don't create job
			if (haulingAllocation != null) {
				messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, haulingAllocation);
			}
		}

	}

	private Entity getMatch(Collection<InventoryComponent.InventoryEntry> inventoryEntries, List<ItemTypeWithMaterial> inputItemOptions) {
		for (InventoryComponent.InventoryEntry entry : inventoryEntries) {
			if (matches(entry.entity, inputItemOptions)) {
				ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
				if (itemAllocationComponent.getNumUnallocated() > 0) {
					return entry.entity;
				}
			}
		}

		return null;
	}

	private boolean matches(Entity itemEntity, List<ItemTypeWithMaterial> inputItemOptions) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		for (ItemTypeWithMaterial inputItemOption : inputItemOptions) {
			if (inputItemOption.getMaterial() != null) {
				if (attributes.getItemType().equals(inputItemOption.getItemType()) &&
						attributes.getMaterial(inputItemOption.getMaterial().getMaterialType()).equals(inputItemOption.getMaterial())) {
					return true;
				}
			} else {
				// material type doesn't matter
				if (attributes.getItemType().equals(inputItemOption.getItemType())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean allIngredientsInPlace(CookingSession cookingSession) {
		boolean liquidComplete = false;
		boolean ingredientsComplete = false;

		if (cookingSession.getRecipe().getInputLiquidQuantity() > 0) {
			LiquidContainerComponent liquidContainerComponent = cookingSession.getAssignedFurnitureEntity().getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent == null) {
				Logger.error("Could not find required liquid container in assigned furniture entity ("+cookingSession.getAssignedFurnitureEntity()+") for cooking session with recipe " + cookingSession.getRecipe().getRecipeName());
			} else {
				if (liquidContainerComponent.getLiquidQuantity() >= cookingSession.getRecipe().getInputLiquidQuantity()) {
					// FIXME not checking liquid material
					liquidComplete = true;
				}
			}
		} else {
			liquidComplete = true;
		}

		if (cookingSession.getRecipe().getInputItemQuantity() > 0) {
			int ingredientCount = 0;
			InventoryComponent inventoryComponent = cookingSession.getAssignedFurnitureEntity().getOrCreateComponent(InventoryComponent.class);
			for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
				if (entry.entity.getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes();
					ingredientCount += attributes.getQuantity();
				}
			}

			if (ingredientCount >= cookingSession.getRecipe().getInputItemQuantity()) {
				ingredientsComplete = true;
			}
		} else {
			ingredientsComplete = true;
		}

		return liquidComplete && ingredientsComplete;
	}

	private void addCookingJob(CookingSession cookingSession) {
		Job cookingJob = new Job(cookingJobType);
		cookingJob.setJobPriority(priority);
		cookingJob.setCookingRecipe(cookingSession.getRecipe());
		cookingJob.setJobLocation(toGridPoint(cookingSession.getAssignedFurnitureEntity().getLocationComponent().getWorldPosition()));
		cookingJob.setTargetId(cookingSession.getAssignedFurnitureEntity().getId());

		cookingSession.setCookingJob(cookingJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, cookingJob);
	}


	@Override
	public void tileRemoved(GridPoint2 location) {
		// Don't need to do anything, list of furniture entities is updated every cycle
	}

	public void setCookingRecipes(Collection<CookingRecipe> cookingRecipes) {
		this.cookingRecipes = cookingRecipes;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.JOB_COMPLETED: {
				handleJobCompleted((JobCompletedMessage)msg.extraInfo);
				return false; // Not the primary handler for this message
			}
			case MessageType.JOB_CANCELLED: {
				handleJobCancelled((Job) msg.extraInfo);
				return false; // Not the primary handler for this message
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handleJobCancelled(Job cancelledJob) {
		for (CookingSession cookingSession : cookingSessions.values()) {
			cookingSession.getInputIngredientJobs().removeIf(job -> job.equals(cancelledJob));
			if (cookingSession.getCookingJob() != null && cookingSession.getCookingJob().equals(cancelledJob)) {
				cookingSession.setCookingJob(null);
			}
		}
	}

	private void handleJobCompleted(JobCompletedMessage jobCompletedMessage) {
		Job completedJob = jobCompletedMessage.getJob();
		if (completedJob != null && completedJob.getType().equals(cookingJobType)) {
			for (CookingSession cookingSession : cookingSessions.values()) {
				if (completedJob.equals(cookingSession.getCookingJob())) {
					cookingSession.setCompleted(true);
					break;
				}
			}
		}
	}

	public void setRequiredProfession(Skill requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public Skill getRequiredProfession() {
		return requiredProfession;
	}

	public void setJobTypes(JobType cookingJobType, JobType transferLiquidJobType, JobType haulingJobType) {
		this.cookingJobType = cookingJobType;
		this.transferLiquidJobType = transferLiquidJobType;
		this.haulingJobType = haulingJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("cookingJobType", cookingJobType.getName());
		asJson.put("transferLiquidJobType", transferLiquidJobType.getName());
		asJson.put("haulingJobType", haulingJobType.getName());

		if (!cookingRecipes.isEmpty()) {
			JSONArray recipesJson = new JSONArray();
			for (CookingRecipe cookingRecipe : cookingRecipes) {
				recipesJson.add(cookingRecipe.getRecipeName());
			}
			asJson.put("recipes", recipesJson);
		}

		if (!cookingSessions.isEmpty()) {
			JSONArray cookingSessionsJson = new JSONArray();
			for (CookingSession cookingSession : cookingSessions.values()) {
				JSONObject sessionJson = new JSONObject(true);
				cookingSession.writeTo(sessionJson, savedGameStateHolder);
				cookingSessionsJson.add(sessionJson);
			}
			asJson.put("sessions", cookingSessionsJson);
		}

		if (!furnitureEntities.isEmpty()) {
			JSONArray furnitureJson = new JSONArray();
			for (Entity entity : furnitureEntities.values()) {
				entity.writeTo(savedGameStateHolder);
				furnitureJson.add(entity.getId());
			}
			asJson.put("furniture", furnitureJson);
		}

		if (requiredProfession != null) {
			asJson.put("profession", requiredProfession.getName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.cookingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("cookingJobType"));
		if (cookingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("cookingJobType"));
		}
		this.transferLiquidJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("transferLiquidJobType"));
		if (transferLiquidJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("transferLiquidJobType"));
		}
		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}


		JSONArray recipesJson = asJson.getJSONArray("recipes");
		cookingRecipes = new LinkedList<>();
		if (recipesJson != null) {
			for (Object recipeName : recipesJson) {
				CookingRecipe cookingRecipe = relatedStores.cookingRecipeDictionary.getByName((String) recipeName);
				if (cookingRecipe == null) {
					throw new InvalidSaveException("Could not find recipe by name " + recipeName);
				} else {
					this.cookingRecipes.add(cookingRecipe);
				}
			}
		}

		JSONArray sessionsJson = asJson.getJSONArray("sessions");
		if (sessionsJson != null) {
			for (Object sessionJson : sessionsJson) {
				CookingSession cookingSession = new CookingSession();
				cookingSession.readFrom((JSONObject)sessionJson, savedGameStateHolder, relatedStores);
				cookingSessions.put(cookingSession.getAssignedFurnitureEntity().getId(), cookingSession);
			}
		}

		JSONArray furnitureJson = asJson.getJSONArray("furniture");
		if (furnitureJson != null) {
			for (int cursor = 0; cursor < furnitureJson.size(); cursor++) {
				Entity entity = savedGameStateHolder.entities.get(furnitureJson.getLongValue(cursor));
				if (entity == null) {
					throw new InvalidSaveException("Could not find entity by ID " + furnitureJson.getLongValue(cursor));
				} else {
					furnitureEntities.put(entity.getId(), entity);
				}
			}
		}

		String requiredProfessionName = asJson.getString("profession");
		if (requiredProfessionName != null) {
			this.requiredProfession = relatedStores.skillDictionary.getByName(requiredProfessionName);
			if (this.requiredProfession == null) {
				throw new InvalidSaveException("Could not find profession by name " + requiredProfessionName);
			}
		}
	}
}
