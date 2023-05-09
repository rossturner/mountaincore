package technology.rocketjump.mountaincore.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.combat.EnteringCombatException;
import technology.rocketjump.mountaincore.entities.ai.combat.ExitingCombatException;
import technology.rocketjump.mountaincore.entities.ai.goap.*;
import technology.rocketjump.mountaincore.entities.ai.goap.condition.GoalSelectionCondition;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.creature.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Sanity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.Blinded;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.TemporaryBlinded;
import technology.rocketjump.mountaincore.environment.model.WeatherType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness.*;

public class CreatureBehaviour implements BehaviourComponent, Destructible, SelectableDescription {

	private static final int MAX_TANTRUMS = 3;

	protected SteeringComponent steeringComponent = new SteeringComponent();
	protected CombatBehaviour combatBehaviour = new CombatBehaviour();

	protected Entity parentEntity;
	protected MessageDispatcher messageDispatcher;
	protected GameContext gameContext;

	protected GoalDictionary goalDictionary;

	protected CreatureGroup creatureGroup;
	protected AssignedGoal currentGoal;
	protected final GoalQueue goalQueue = new GoalQueue();
	protected transient double lastUpdateGameTime;
	protected static final int DISTANCE_TO_LOOK_AROUND = 7;

	private float stunTime;

	public void constructWith(GoalDictionary goalDictionary) {
		this.goalDictionary = goalDictionary;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
		steeringComponent.init(parentEntity, gameContext.getAreaMap(), messageDispatcher);
		combatBehaviour.init(parentEntity, messageDispatcher, gameContext);

		if (currentGoal != null) {
			currentGoal.init(parentEntity, messageDispatcher, gameContext);
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
		if (creatureGroup != null) {
			creatureGroup.removeMemberId(parentEntity.getId());
		}
		combatBehaviour.destroy(parentEntity, messageDispatcher, gameContext);
	}

	@Override
	public void update(float deltaTime) {
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
		}

		CombatStateComponent combatState = parentEntity.getComponent(CombatStateComponent.class);
		if (combatState.isInCombat()) {
			try {
				combatBehaviour.update(deltaTime);
				return;
			} catch (ExitingCombatException e) {
				combatBehaviour.onExitingCombat();
				messageDispatcher.dispatchMessage(MessageType.CREATURE_EXITING_COMBAT, parentEntity);
			}
		}


		if (currentGoal == null || currentGoal.isComplete()) {
			try {
				currentGoal = pickNextGoalFromQueue();
			} catch (EnteringCombatException e) {
				combatBehaviour.onEnteringCombat();
				// Currently assuming whatever threw this has first cleared the CombatStateComponent
				// Only switch to combat when we need to pick a new goal - previous goal may have needed to clean up interrupted actions
				messageDispatcher.dispatchMessage(MessageType.CREATURE_ENTERING_COMBAT, parentEntity);
				return;
			}
		}

		try {
			currentGoal.update(deltaTime, gameContext);
		} catch (SwitchGoalException e) {
			AssignedGoal newGoal = new AssignedGoal(e.target, parentEntity, messageDispatcher, gameContext);
			newGoal.setAssignedJob(currentGoal.getAssignedJob());
			newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedHaulingAllocation());
			newGoal.setLiquidAllocation(currentGoal.getLiquidAllocation());
			if (newGoal.getAssignedHaulingAllocation() == null && currentGoal.getAssignedJob() != null) {
				newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedJob().getHaulingAllocation());
			}
			currentGoal = newGoal;
		}
	}

	@Override
	public void updateWhenPaused() {
		CombatStateComponent combatState = parentEntity.getComponent(CombatStateComponent.class);
		if (combatState.isInCombat()) {
			combatBehaviour.updateWhenPaused();
		} else if (currentGoal != null && !currentGoal.isComplete()) {
			currentGoal.updateWhenPaused();
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = gameTime - lastUpdateGameTime;
		lastUpdateGameTime = gameTime;

		if (creatureGroup != null) {
			creatureGroup.infrequentUpdate(gameContext, messageDispatcher);
		}

		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		needsComponent.update(elapsed, parentEntity, messageDispatcher);

		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
		boolean isInMilitary = militaryComponent != null && militaryComponent.isInMilitary();

		if (happinessComponent != null && !isInMilitary) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
			if (currentTile != null && currentTile.getRoof().getState().equals(TileRoofState.OPEN) &&
					gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(WeatherType.HappinessInteraction.STANDING)) {
				happinessComponent.add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(WeatherType.HappinessInteraction.STANDING));
			}
		}

		addGoalsToQueue(gameContext);
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		if (attributes.getConsciousness().equals(AWAKE)) {
			lookAtNearbyThings(gameContext);
		}

		if (attributes.getRace().getBehaviour().getIsSapient() && !isInMilitary) {
			if (attributes.getSanity().equals(Sanity.SANE) && attributes.getConsciousness().equals(AWAKE) &&
					happinessComponent != null && happinessComponent.getNetModifier() <= HappinessComponent.MIN_HAPPINESS_VALUE) {
				messageDispatcher.dispatchMessage(MessageType.SAPIENT_CREATURE_INSANITY, parentEntity);
			}
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

	protected AssignedGoal pickNextGoalFromQueue() throws EnteringCombatException {
		if (inVehicleAndNotDriving()) {
			return AssignedGoalFactory.doNothingGoal(parentEntity, messageDispatcher, gameContext);
		}

		if (parentEntity.isOnFire()) {
			return AssignedGoalFactory.onFireGoal(parentEntity, messageDispatcher, gameContext);
		}

		Optional<Memory> combatMemory = getCombatRelatedMemory(parentEntity, creatureGroup, gameContext);
		if (combatMemory.isPresent()) {
			if (creatureGroup != null) {
				creatureGroup.getSharedMemoryComponent().addShortTerm(combatMemory.get(), gameContext.getGameClock());
			}
			CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
			combatStateComponent.clearState();
			combatStateComponent.setTargetedOpponentId(combatMemory.get().getRelatedEntityId());
			if (combatMemory.get().getRelatedEntityIds() != null) {
				combatStateComponent.setOpponentEntityIds(combatMemory.get().getRelatedEntityIds());
			}
			throw new EnteringCombatException();
		}

		MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);

		// (Override) if we're hauling an item, need to place it
		if (parentEntity.getComponent(HaulingComponent.class) != null && parentEntity.getComponent(HaulingComponent.class).getHauledEntity() != null) {
			return AssignedGoalFactory.placeHauledItemGoal(parentEntity, messageDispatcher, gameContext);
		}

		Optional<Memory> breakdownMemory = memoryComponent.getShortTermMemories(gameContext.getGameClock())
				.stream().filter(m -> m.getType().equals(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN)).findFirst();
		if (breakdownMemory.isPresent()) {
			memoryComponent.removeByType(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN);
			long previousTantrums = memoryComponent.getLongTermMemories().stream().filter(m -> m.getType().equals(MemoryType.HAD_A_TANTRUM)).count();
			if (previousTantrums < MAX_TANTRUMS) {
				messageDispatcher.dispatchMessage(MessageType.SETTLER_TANTRUM, parentEntity);
				return AssignedGoalFactory.tantrumGoal(parentEntity, messageDispatcher, gameContext);
			} else {
				messageDispatcher.dispatchMessage(MessageType.SAPIENT_CREATURE_INSANITY, parentEntity);
				return new AssignedGoal(SpecialGoal.IDLE.getInstance(), parentEntity, messageDispatcher, gameContext);
			}
		}

		AssignedGoal placeInventoryItemsGoal = AssignedGoalFactory.checkToPlaceInventoryItems(parentEntity, messageDispatcher, gameContext);
		if (placeInventoryItemsGoal != null) {
			return placeInventoryItemsGoal;
		}

		if (creatureGroup != null && creatureGroup instanceof InvasionCreatureGroup invasionCreatureGroup) {
			SpecialGoal specialGoal = invasionCreatureGroup.popSpecialGoal();
			if (specialGoal != null) {
				return new AssignedGoal(specialGoal.getInstance(), parentEntity, messageDispatcher, gameContext);
			}
		} else if (creatureGroup != null && creatureGroup instanceof TraderCreatureGroup traderCreatureGroup) {
			SpecialGoal specialGoal = traderCreatureGroup.popSpecialGoal();
			if (specialGoal != null) {
				return new AssignedGoal(specialGoal.getInstance(), parentEntity, messageDispatcher, gameContext);
			}
		}

		List<ScheduleCategory> currentScheduleCategories = getCurrentSchedule().getCurrentApplicableCategories(gameContext.getGameClock());
		QueuedGoal nextGoal = goalQueue.popNextGoal(currentScheduleCategories);
		if (nextGoal == null) {
			return new AssignedGoal(SpecialGoal.IDLE.getInstance(), parentEntity, messageDispatcher, gameContext);
		}
		return new AssignedGoal(nextGoal.getGoal(), parentEntity, messageDispatcher, gameContext);
	}

	private static final List<MemoryType> enterCombatMemoryTypes = List.of(MemoryType.ATTACKED_BY_CREATURE, MemoryType.ABOUT_TO_ATTACK_CREATURE);

	public static Optional<Memory> getCombatRelatedMemory(Entity entity, CreatureGroup creatureGroup, GameContext gameContext) {
		MemoryComponent memoryComponent = entity.getOrCreateComponent(MemoryComponent.class);
		Optional<Memory> combatMemory = memoryComponent.getShortTermMemories(gameContext.getGameClock())
				.stream().filter(m -> enterCombatMemoryTypes.contains(m.getType())).findAny();
		if (combatMemory.isEmpty() && creatureGroup != null) {
			combatMemory = creatureGroup.getSharedMemoryComponent().getShortTermMemories(gameContext.getGameClock())
					.stream().filter(m -> enterCombatMemoryTypes.contains(m.getType())).findAny();
		}
		return combatMemory;
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
		if (creatureGroup != null) {
			creatureGroup.addMemberId(parentEntity.getId());
		}
	}

	protected void addGoalsToQueue(GameContext gameContext) {
		CreatureCategory currentCreatureCategory = CreatureCategory.getCategoryFor(parentEntity);
		goalQueue.removeExpiredGoals(gameContext.getGameClock());
		for (Goal potentialGoal : goalDictionary.getAllGoals()) {
			if (potentialGoal.getSelectors().isEmpty()) {
				continue; // Don't add goals with no selectors
			}
			if (!potentialGoal.creatureCategories.contains(currentCreatureCategory)) {
				// Goal does not apply to our settler category
				continue;
			}
			if (currentGoal != null && potentialGoal.equals(currentGoal.goal)) {
				continue; // Don't queue up the current goal
			}
			for (GoalSelector selector : potentialGoal.getSelectors()) {
				boolean allConditionsApply = true;
				for (GoalSelectionCondition condition : selector.conditions) {
					if (!condition.apply(parentEntity, gameContext)) {
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

	public void militaryAssignmentChanged() {
		goalQueue.clear();

		if (currentGoal != null && !currentGoal.isComplete() && !currentGoal.goal.creatureCategories.contains(CreatureCategory.getCategoryFor(parentEntity))) {
			currentGoal.setInterrupted(true);
		}

		addGoalsToQueue(gameContext);
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

		Faction myFaction = parentEntity.getOrCreateComponent(FactionComponent.class).getFaction();
		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);

		GridPoint2 parentPosition = VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition());
		for (CompassDirection compassDirection : CompassDirection.values()) {
			for (int distance = 1; distance <= DISTANCE_TO_LOOK_AROUND; distance++) {
				GridPoint2 targetPosition = parentPosition.cpy().add(compassDirection.getXOffset() * distance, compassDirection.getYOffset() * distance);
				MapTile targetTile = gameContext.getAreaMap().getTile(targetPosition);
				if (targetTile == null || targetTile.hasWall()) {
					// Stop looking in this direction
					break;
				}

				for (Entity entityInTile : targetTile.getEntities()) {
					if (entityInTile.getType().equals(EntityType.CREATURE)) {

						CreatureEntityAttributes creatureEntityAttributes = (CreatureEntityAttributes) entityInTile.getPhysicalEntityComponent().getAttributes();
						if (creatureEntityAttributes.getConsciousness().equals(DEAD)
								&& creatureEntityAttributes.getRace().equals(((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace())) {
							// Saw a dead body!
							if (happinessComponent != null) {
								happinessComponent.add(HappinessComponent.HappinessModifier.SAW_DEAD_BODY);
							}

						}

						if (!creatureEntityAttributes.getConsciousness().equals(AWAKE)) {
							continue;
						}

						Faction targetFaction = entityInTile.getOrCreateComponent(FactionComponent.class).getFaction();
						MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
						if (hostileFactions(myFaction, targetFaction)) {
							Memory attackCreatureMemory = new Memory(MemoryType.ABOUT_TO_ATTACK_CREATURE, gameContext.getGameClock());
							attackCreatureMemory.setRelatedEntityId(entityInTile.getId());
							memoryComponent.addShortTerm(attackCreatureMemory, gameContext.getGameClock());
						}
					}
				}

			}
		}
	}

	public boolean hostileFactions(Faction myFaction, Faction targetFaction) {
		return (myFaction == Faction.MONSTERS && myFaction != targetFaction) ||
				(myFaction == Faction.SETTLEMENT && (targetFaction == Faction.MONSTERS || targetFaction == Faction.HOSTILE_INVASION));
	}


	private boolean inVehicleAndNotDriving() {
		return parentEntity.getContainingVehicle() != null && !parentEntity.isDrivingVehicle();
	}

	public void applyStun(Random random) {
		this.stunTime = 1f + (random.nextFloat() * 3f);
	}

	public boolean isStunned() {
		return this.stunTime > 0f;
	}

	public Schedule getCurrentSchedule() {
		MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.isInMilitary() && militaryComponent.getSquadId() != null) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null) {
				return ScheduleDictionary.getScheduleForSquadShift(squad.getShift());
			}
		}
		if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes attributes) {
			return attributes.getRace().getBehaviour().getSchedule();
		} else {
			Logger.error("Looking for a schedule on " + this.getClass().getSimpleName() + " with no " + CreatureEntityAttributes.class.getSimpleName());
			return ScheduleDictionary.NULL_SCHEDULE;
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
			return List.of(i18nTranslator.getTranslatedString("ACTION.KNOCKED_UNCONSCIOUS"));
		}

		List<I18nText> descriptionStrings = new ArrayList<>();

		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent != null && combatStateComponent.isInCombat()) {
			descriptionStrings.addAll(combatBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher));
		} else {
			descriptionStrings.add(i18nTranslator.getCurrentGoalDescription(parentEntity, currentGoal, gameContext));
		}
		if (stunTime > 0) {
			descriptionStrings.add(i18nTranslator.getTranslatedString("ACTION.STUNNED"));
		}
		return descriptionStrings;
	}

	public CombatBehaviour getCombatBehaviour() {
		return combatBehaviour;
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

		JSONObject combatBehaviourJson = new JSONObject(true);
		combatBehaviour.writeTo(combatBehaviourJson, savedGameStateHolder);
		asJson.put("combatBehaviour", combatBehaviourJson);

		if (stunTime > 0) {
			asJson.put("stunTime", stunTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.goalDictionary = relatedStores.goalDictionary;

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

		this.combatBehaviour.readFrom(asJson.getJSONObject("combatBehaviour"), savedGameStateHolder, relatedStores);

		this.stunTime = asJson.getFloatValue("stunTime");
	}
}
