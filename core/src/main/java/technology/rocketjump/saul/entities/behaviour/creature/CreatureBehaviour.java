package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.ai.goap.*;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.*;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.creature.Sanity;
import technology.rocketjump.saul.entities.model.physical.creature.status.Blinded;
import technology.rocketjump.saul.entities.model.physical.creature.status.TemporaryBlinded;
import technology.rocketjump.saul.entities.model.physical.item.AmmoType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.roof.TileRoofState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static technology.rocketjump.saul.entities.ai.goap.SpecialGoal.IDLE;
import static technology.rocketjump.saul.entities.behaviour.creature.AssignedGoalFactory.*;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.HappinessModifier.SAW_DEAD_BODY;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.MIN_HAPPINESS_VALUE;
import static technology.rocketjump.saul.entities.model.EntityType.CREATURE;
import static technology.rocketjump.saul.entities.model.EntityType.ITEM;
import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.*;
import static technology.rocketjump.saul.environment.model.WeatherType.HappinessInteraction.STANDING;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class CreatureBehaviour implements BehaviourComponent, Destructible, SelectableDescription {

	private static final int MAX_TANTRUMS = 3;

	protected SteeringComponent steeringComponent = new SteeringComponent();

	protected Entity parentEntity;
	protected MessageDispatcher messageDispatcher;
	protected GameContext gameContext;

	protected GoalDictionary goalDictionary;
	protected RoomStore roomStore;

	protected CreatureGroup creatureGroup;
	protected AssignedGoal currentGoal;
	protected final GoalQueue goalQueue = new GoalQueue();
	protected transient double lastUpdateGameTime;
	protected static final int DISTANCE_TO_LOOK_AROUND = 5;

	private float stunTime;

	public void constructWith(GoalDictionary goalDictionary, RoomStore roomStore) {
		this.goalDictionary = goalDictionary;
		this.roomStore = roomStore;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
		steeringComponent.init(parentEntity, gameContext.getAreaMap(), parentEntity.getLocationComponent(), messageDispatcher);

		if (currentGoal != null) {
			currentGoal.init(parentEntity, messageDispatcher);
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented " + this.getClass().getSimpleName() + ".clone()");
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (currentGoal != null) {
			currentGoal.destroy(parentEntity, messageDispatcher, gameContext);
			currentGoal = null;
		}
	}

	@Override
	public void update(float deltaTime) {
		if (currentGoal == null || currentGoal.isComplete()) {
			currentGoal = pickNextGoalFromQueue();
		}

		// Not going to update steering when asleep so can't be pushed around
		Consciousness consciousness = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness();
		if (AWAKE.equals(consciousness)) {
			steeringComponent.update(deltaTime);
		} else if (KNOCKED_UNCONSCIOUS.equals(consciousness)) {
			return;
		}

		if (stunTime > 0) {
			stunTime -= deltaTime;
			if (stunTime < 0) {
				stunTime = 0;
			}
			return;
		}

		try {
			currentGoal.update(deltaTime, gameContext);
		} catch (SwitchGoalException e) {
			AssignedGoal newGoal = new AssignedGoal(e.target, parentEntity, messageDispatcher);
			newGoal.setAssignedJob(currentGoal.getAssignedJob());
			newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedHaulingAllocation());
			newGoal.setLiquidAllocation(currentGoal.getLiquidAllocation());
			if (newGoal.getAssignedHaulingAllocation() == null && currentGoal.getAssignedJob() != null) {
				newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedJob().getHaulingAllocation());
			}
			currentGoal = newGoal;
		}
	}

	public AssignedGoal getCurrentGoal() {
		return currentGoal;
	}


	public void setCurrentGoal(AssignedGoal assignedGoal) {
		this.currentGoal = assignedGoal;
	}

	public GoalQueue getGoalQueue() {
		return goalQueue;
	}
	protected AssignedGoal pickNextGoalFromQueue() {
		if (parentEntity.isOnFire()) {
			return onFireGoal(parentEntity, messageDispatcher, gameContext);
		}

		Optional<Memory> attackedMemory = getMemoryOfAttackedByCreature(parentEntity, creatureGroup, gameContext);
		if (attackedMemory.isPresent()) {
			if (creatureGroup != null) {
				creatureGroup.getSharedMemoryComponent().addShortTerm(attackedMemory.get(), gameContext.getGameClock());
			}
			return attackedByCreatureResponse(parentEntity, messageDispatcher, attackedMemory.get());
		}

		MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);

		// (Override) if we're hauling an item, need to place it
		if (parentEntity.getComponent(HaulingComponent.class) != null && parentEntity.getComponent(HaulingComponent.class).getHauledEntity() != null) {
			return placeHauledItemGoal(parentEntity, roomStore, messageDispatcher, gameContext);
		}

		Optional<Memory> breakdownMemory = memoryComponent.getShortTermMemories(gameContext.getGameClock())
				.stream().filter(m -> m.getType().equals(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN)).findFirst();
		if (breakdownMemory.isPresent()) {
			memoryComponent.removeByType(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN);
			long previousTantrums = memoryComponent.getLongTermMemories().stream().filter(m -> m.getType().equals(MemoryType.HAD_A_TANTRUM)).count();
			if (previousTantrums < MAX_TANTRUMS) {
				messageDispatcher.dispatchMessage(MessageType.SETTLER_TANTRUM, parentEntity);
				return tantrumGoal(parentEntity, messageDispatcher, gameContext);
			} else {
				messageDispatcher.dispatchMessage(MessageType.SAPIENT_CREATURE_INSANITY, parentEntity);
				return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
			}
		}

		AssignedGoal placeInventoryItemsGoal = checkToPlaceInventoryItems(parentEntity, roomStore, messageDispatcher, gameContext);
		if (placeInventoryItemsGoal != null) {
			return placeInventoryItemsGoal;
		}

		Schedule schedule = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace().getBehaviour().getSchedule();
		List<ScheduleCategory> currentScheduleCategories = schedule == null ? List.of() : schedule.getCurrentApplicableCategories(gameContext.getGameClock());
		QueuedGoal nextGoal = goalQueue.popNextGoal(currentScheduleCategories);
		if (nextGoal == null) {
			return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
		}
		return new AssignedGoal(nextGoal.getGoal(), parentEntity, messageDispatcher);
	}

	public static Optional<Memory> getMemoryOfAttackedByCreature(Entity entity, CreatureGroup creatureGroup, GameContext gameContext) {
		MemoryComponent memoryComponent = entity.getOrCreateComponent(MemoryComponent.class);
		Optional<Memory> attackedMemory = memoryComponent.getShortTermMemories(gameContext.getGameClock())
				.stream().filter(m -> m.getType().equals(MemoryType.ATTACKED_BY_CREATURE)).findAny();
		if (attackedMemory.isEmpty() && creatureGroup != null) {
			attackedMemory = creatureGroup.getSharedMemoryComponent().getShortTermMemories(gameContext.getGameClock())
					.stream().filter(m -> m.getType().equals(MemoryType.ATTACKED_BY_CREATURE)).findAny();
		}
		return attackedMemory;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = gameTime - lastUpdateGameTime;
		lastUpdateGameTime = gameTime;

		if (creatureGroup != null) {
			creatureGroup.infrequentUpdate(gameContext);
		}

		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		needsComponent.update(elapsed, parentEntity, messageDispatcher);

		parentEntity.getComponent(StatusComponent.class).infrequentUpdate(elapsed);

		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
			if (currentTile != null && currentTile.getRoof().getState().equals(TileRoofState.OPEN) &&
					gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(STANDING)) {
				happinessComponent.add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(STANDING));
			}
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		thinkAboutRequiredEquipment(gameContext);
		addGoalsToQueue(gameContext);

		lookAtNearbyThings(gameContext);

		if (attributes.getRace().getBehaviour().getIsSapient()) {
			if (attributes.getSanity().equals(Sanity.SANE) && attributes.getConsciousness().equals(AWAKE) &&
					happinessComponent != null && happinessComponent.getNetModifier() <= MIN_HAPPINESS_VALUE) {
				messageDispatcher.dispatchMessage(MessageType.SAPIENT_CREATURE_INSANITY, parentEntity);
			}
		}
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return steeringComponent;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		boolean isSapient = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace().getBehaviour().getIsSapient();
		boolean settlementFaction = parentEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(Faction.SETTLEMENT);
		return isSapient && settlementFaction;
	}

	public CreatureGroup getCreatureGroup() {
		return creatureGroup;
	}

	public void setCreatureGroup(CreatureGroup creatureGroup) {
		this.creatureGroup = creatureGroup;
	}

	private void thinkAboutRequiredEquipment(GameContext gameContext) {
		WeaponSelectionComponent weaponSelectionComponent = parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);

		if (weaponSelectionComponent.getSelectedWeapon().isPresent()) {
			ItemType weaponItemType = weaponSelectionComponent.getSelectedWeapon().get();

			InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
			InventoryComponent.InventoryEntry weaponInInventory = inventoryComponent.findByItemType(weaponItemType, gameContext.getGameClock());

			if (weaponInInventory == null) {
				Memory itemRequiredMemory = new Memory(MemoryType.LACKING_REQUIRED_ITEM, gameContext.getGameClock());
				itemRequiredMemory.setRelatedItemType(weaponItemType);
				// Should set required material at some point
				parentEntity.getOrCreateComponent(MemoryComponent.class).addShortTerm(itemRequiredMemory, gameContext.getGameClock());
			} else if (weaponItemType.getWeaponInfo() != null && weaponItemType.getWeaponInfo().getRequiresAmmoType() != null) {
				// check for ammo
				AmmoType requiredAmmoType = weaponItemType.getWeaponInfo().getRequiresAmmoType();

				boolean hasAmmo = inventoryComponent.getInventoryEntries().stream()
						.anyMatch(entry -> entry.entity.getType().equals(ITEM) &&
								requiredAmmoType.equals(((ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes()).getItemType().getIsAmmoType()));

				if (!hasAmmo) {
					Memory itemRequiredMemory = new Memory(MemoryType.LACKING_REQUIRED_ITEM, gameContext.getGameClock());
					itemRequiredMemory.setRelatedAmmoType(requiredAmmoType);
					// Should set required material at some point
					parentEntity.getOrCreateComponent(MemoryComponent.class).addShortTerm(itemRequiredMemory, gameContext.getGameClock());
				}
			}
		}
	}

	protected void addGoalsToQueue(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		MemoryComponent memoryComponent = parentEntity.getComponent(MemoryComponent.class);
		goalQueue.removeExpiredGoals(gameContext.getGameClock());
		for (Goal potentialGoal : goalDictionary.getAllGoals()) {
			if (potentialGoal.getSelectors().isEmpty()) {
				continue; // Don't add goals with no selectors
			}
			if (currentGoal != null && potentialGoal.equals(currentGoal.goal)) {
				continue; // Don't queue up the current goal
			}
			for (GoalSelector selector : potentialGoal.getSelectors()) {
				boolean allConditionsApply = true;
				for (GoalSelectionCondition condition : selector.conditions) {
					if (!condition.apply(gameContext.getGameClock(), needsComponent, memoryComponent)) {
						allConditionsApply = false;
						break;
					}
				}
				if (allConditionsApply) {
					goalQueue.add(new QueuedGoal(potentialGoal, selector.scheduleCategory, selector.priority, gameContext.getGameClock()));
					break;
				}
			}
		}
	}

	private void lookAtNearbyThings(GameContext gameContext) {
		StatusComponent statusComponent = parentEntity.getComponent(StatusComponent.class);
		if (statusComponent.contains(Blinded.class) || statusComponent.contains(TemporaryBlinded.class)) {
			return;
		}

		CreatureEntityAttributes parentAttributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (!parentAttributes.getConsciousness().equals(AWAKE)) {
			return;
		}

		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);

		if (happinessComponent != null) {
			GridPoint2 parentPosition = toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition());
			for (CompassDirection compassDirection : CompassDirection.values()) {
				for (int distance = 1; distance <= DISTANCE_TO_LOOK_AROUND; distance++) {
					GridPoint2 targetPosition = parentPosition.cpy().add(compassDirection.getXOffset() * distance, compassDirection.getYOffset() * distance);
					MapTile targetTile = gameContext.getAreaMap().getTile(targetPosition);
					if (targetTile == null || targetTile.hasWall()) {
						// Stop looking in this direction
						break;
					}

					for (Entity entityInTile : targetTile.getEntities()) {
						if (entityInTile.getType().equals(CREATURE)) {
							CreatureEntityAttributes creatureEntityAttributes = (CreatureEntityAttributes) entityInTile.getPhysicalEntityComponent().getAttributes();
							if (creatureEntityAttributes.getConsciousness().equals(DEAD)
									&& creatureEntityAttributes.getRace().equals(((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace())) {
								// Saw a dead body!
								happinessComponent.add(SAW_DEAD_BODY);

								return; // TODO remove this, but for now this is the only thing to see so might as well stop looking
							}
						}
					}

				}
			}
		}

	}

	public void applyStun(Random random) {
		this.stunTime = 1f + (random.nextFloat() * 3f);
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
			return List.of(i18nTranslator.getTranslatedString("ACTION.KNOCKED_UNCONSCIOUS"));
		}

		List<I18nText> descriptionStrings = new ArrayList<>();
		descriptionStrings.add(i18nTranslator.getCurrentGoalDescription(parentEntity, currentGoal, gameContext));
		if (stunTime > 0) {
			descriptionStrings.add(i18nTranslator.getTranslatedString("ACTION.STUNNED"));
		}
		return descriptionStrings;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (creatureGroup != null) {
			creatureGroup.writeTo(savedGameStateHolder);
			asJson.put("creatureGroup", creatureGroup.getGroupId());
		}

		if (currentGoal != null) {
			JSONObject currentGoalJson = new JSONObject(true);
			currentGoal.writeTo(currentGoalJson, savedGameStateHolder);
			asJson.put("currentGoal", currentGoalJson);
		}

		if (!goalQueue.isEmpty()) {
			JSONObject goalQueueJson = new JSONObject(true);
			goalQueue.writeTo(goalQueueJson, savedGameStateHolder);
			asJson.put("goalQueue", goalQueueJson);
		}

		if (steeringComponent != null) {
			JSONObject steeringComponentJson = new JSONObject(true);
			steeringComponent.writeTo(steeringComponentJson, savedGameStateHolder);
			asJson.put("steeringComponent", steeringComponentJson);
		}

		if (stunTime > 0) {
			asJson.put("stunTime", stunTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.goalDictionary = relatedStores.goalDictionary;
		this.roomStore = relatedStores.roomStore;

		Long creatureGroupId = asJson.getLong("creatureGroup");
		if (creatureGroupId != null) {
			this.creatureGroup = savedGameStateHolder.creatureGroups.get(creatureGroupId);
			if (this.creatureGroup == null) {
				throw new InvalidSaveException("Could not find creature group with ID " + creatureGroupId);
			}
		}

		JSONObject currentGoalJson = asJson.getJSONObject("currentGoal");
		if (currentGoalJson != null) {
			currentGoal = new AssignedGoal();
			currentGoal.readFrom(currentGoalJson, savedGameStateHolder, relatedStores);
		}

		JSONObject goalQueueJson = asJson.getJSONObject("goalQueue");
		if (goalQueueJson != null) {
			goalQueue.readFrom(goalQueueJson, savedGameStateHolder, relatedStores);
		}

		JSONObject steeringComponentJson = asJson.getJSONObject("steeringComponent");
		if (steeringComponentJson != null) {
			this.steeringComponent.readFrom(steeringComponentJson, savedGameStateHolder, relatedStores);
		}

		this.stunTime = asJson.getFloatValue("stunTime");
	}
}
