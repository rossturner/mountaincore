package technology.rocketjump.saul.entities.ai.goap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.cooking.model.FoodAllocation;
import technology.rocketjump.saul.entities.ai.goap.actions.*;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import java.util.*;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

/**
 * This class acts as a way of keeping state across Actions for a Goal
 */
public class AssignedGoal implements ChildPersistable, Destructible {

	private static final Double MIN_NEED_BEFORE_GOAL_INTERRUPTED = 0.12;
	public Entity parentEntity;
	public MessageDispatcher messageDispatcher;

	public Goal goal;
	public Deque<Action> actionQueue = new ArrayDeque<>();
	private Job assignedJob;
	private HaulingAllocation assignedHaulingAllocation;
	private FoodAllocation foodAllocation;
	private LiquidAllocation liquidAllocation;
	private Long assignedFurnitureId;
	private Vector2 targetLocation; // Only used for facing toward currently
	private Memory relevantMemory;
	private boolean interrupted; // For if the entire goal should be cancelled, but needs to deal with any cleanup

	private transient List<Action> previousActions = new LinkedList<>(); // For debugging only right now
	private transient AssignedGoal parentGoal;

	public AssignedGoal() {

	}

	public AssignedGoal(Goal goal, Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.goal = goal;
		init(parentEntity, messageDispatcher);
		for (Class<? extends Action> initialActionClass : goal.getInitialActions()) {
			actionQueue.add(Action.newInstance(initialActionClass, this));
		}
	}

	public void init(Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;

		for (Action action : actionQueue) {
			if (action instanceof InitialisableAction) {
				((InitialisableAction)action).init();
			}
		}
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		Action currentAction = getCurrentAction();
		while (currentAction != null) {
			if (currentAction.isInterruptible()) {
				currentAction.actionInterrupted(gameContext);
			}
			try {
				checkForActionCompletionOrElseUpdate(currentAction, 0.1f, gameContext);
			} catch (SwitchGoalException e) {
				// Do nothing, stop here?
			}
			currentAction = getCurrentAction();
		}

		if (assignedJob != null) {
			messageDispatcher.dispatchMessage(MessageType.JOB_ASSIGNMENT_CANCELLED, assignedJob);
		}
		if (assignedHaulingAllocation != null) {
			messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, assignedHaulingAllocation);
		}
		if (foodAllocation != null) {
			messageDispatcher.dispatchMessage(MessageType.FOOD_ALLOCATION_CANCELLED, foodAllocation);
		}
		if (liquidAllocation != null) {
			messageDispatcher.dispatchMessage(MessageType.LIQUID_ALLOCATION_CANCELLED, liquidAllocation);
		}
		if (assignedFurnitureId != null) {
			UnassignFurnitureAction unassignFurnitureAction = new UnassignFurnitureAction(this);
			unassignFurnitureAction.update(0f, gameContext);
		}
	}

	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (actionQueue.isEmpty()) {
			return;
		}

		Action currentAction = actionQueue.peek();

		if (goal.interruptedByCombat) {
			MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
			if (memoryComponent.getShortTermMemories(gameContext.getGameClock()).stream().anyMatch(m -> m.getType().equals(MemoryType.ATTACKED_BY_CREATURE) || m.getType().equals(MemoryType.ABOUT_TO_ATTACK_CREATURE))) {
				setInterrupted(true);
			}
		}
		if (goal.interruptedByLowNeeds) {
			NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
			if (needsComponent != null) {
				for (Map.Entry<EntityNeed, Double> entry : needsComponent.getAll()) {
					if (entry.getValue() < MIN_NEED_BEFORE_GOAL_INTERRUPTED) {
						setInterrupted(true);
						break;
					}
				}
			}
		}

		checkForActionCompletionOrElseUpdate(currentAction, deltaTime, gameContext);
	}

	public void updateWhenPaused() {
		if (actionQueue.isEmpty()) {
			return;
		}

		Action currentAction = actionQueue.peek();
		if (currentAction instanceof WorkOnJobAction workOnJobAction) {
			workOnJobAction.updateProgressBarEffect();
		}
	}

	private void checkForActionCompletionOrElseUpdate(Action currentAction, float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Action.CompletionType actionCompletion = currentAction.isCompleted(gameContext);
		if (actionCompletion == null) {
			if (this.interrupted && currentAction.isInterruptible()) {
				currentAction.actionInterrupted(gameContext);
			} else {
				currentAction.update(deltaTime, gameContext);
			}
		} else {
			previousActions.add(actionQueue.poll());
			ActionTransitions transitions = goal.getAllTransitions().get(currentAction.getClass());
			if (transitions != null) { // Might be null when extra actions added by code
				if (actionCompletion.equals(SUCCESS)) {
					addToQueue(transitions.onSuccess, gameContext);
				} else if (actionCompletion.equals(FAILURE)) {
					addToQueue(transitions.onFailure, gameContext);
				}
			}
		}
	}

	public Action getCurrentAction() {
		return actionQueue.peek();
	}

	private void addToQueue(List<Class<? extends Action>> actionTypeList, GameContext gameContext) {
		for (Class<? extends Action> actionType : actionTypeList) {
			Action action = Action.newInstance(actionType, this);
			if (action.isApplicable(gameContext)) {
				actionQueue.add(action);
			}
		}
	}

	public boolean isComplete() {
		return actionQueue.isEmpty();
	}

	public Job getAssignedJob() {
		return assignedJob;
	}

	public void setAssignedJob(Job assignedJob) {
		this.assignedJob = assignedJob;
		if (assignedJob == null) {
			this.targetLocation = null;
		} else {
			this.targetLocation = toVector(assignedJob.getJobLocation());
		}
	}

	public HaulingAllocation getAssignedHaulingAllocation() {
		return assignedHaulingAllocation;
	}

	public void setAssignedHaulingAllocation(HaulingAllocation assignedHaulingAllocation) {
		this.assignedHaulingAllocation = assignedHaulingAllocation;
	}

	public Vector2 getTargetLocation() {
		return targetLocation;
	}

	public void setTargetLocation(Vector2 targetLocation) {
		this.targetLocation = targetLocation;
	}

	public void setAssignedFurnitureId(Long assignedFurnitureId) {
		this.assignedFurnitureId = assignedFurnitureId;
	}

	public Long getAssignedFurnitureId() {
		return assignedFurnitureId;
	}

	public Memory getRelevantMemory() {
		return relevantMemory;
	}

	public void setRelevantMemory(Memory relevantMemory) {
		this.relevantMemory = relevantMemory;
	}

	public void setFoodAllocation(FoodAllocation foodAllocation) {
		this.foodAllocation = foodAllocation;
	}

	public FoodAllocation getFoodAllocation() {
		return foodAllocation;
	}

	public LiquidAllocation getLiquidAllocation() {
		return liquidAllocation;
	}

	public void setLiquidAllocation(LiquidAllocation liquidAllocation) {
		this.liquidAllocation = liquidAllocation;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public void setParentGoal(AssignedGoal parentGoal) {
		this.parentGoal = parentGoal;
	}

	public AssignedGoal getParentGoal() {
		return parentGoal;
	}

	@Override
	public String toString() {
		return goal.toString();
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("goal", goal.name);

		if (!actionQueue.isEmpty()) {
			JSONArray actionsJson = new JSONArray();
			for (Action action : actionQueue) {
				JSONObject actionJson = new JSONObject(true);
				actionJson.put("name", action.getSimpleName());
				action.writeTo(actionJson, savedGameStateHolder);
				actionsJson.add(actionJson);
			}
			asJson.put("actions", actionsJson);
		}

		if (assignedJob != null) {
			assignedJob.writeTo(savedGameStateHolder);
			asJson.put("assignedJob", assignedJob.getJobId());
		}

		if (assignedHaulingAllocation != null) {
			assignedHaulingAllocation.writeTo(savedGameStateHolder);
			asJson.put("haulingAllocation", assignedHaulingAllocation.getHaulingAllocationId());
		}

		if (foodAllocation != null) {
			JSONObject foodAllocationJson = new JSONObject(true);
			foodAllocation.writeTo(foodAllocationJson, savedGameStateHolder);
			asJson.put("foodAllocation", foodAllocationJson);
		}

		if (liquidAllocation != null) {
			liquidAllocation.writeTo(savedGameStateHolder);
			asJson.put("liquidAllocation", liquidAllocation.getLiquidAllocationId());
		}

		if (assignedFurnitureId != null) {
			asJson.put("assignedFurnitureId", assignedFurnitureId);
		}

		if (targetLocation != null) {
			asJson.put("targetLocation", JSONUtils.toJSON(targetLocation));
		}

		if (relevantMemory != null) {
			JSONObject memoryJson = new JSONObject(true);
			relevantMemory.writeTo(memoryJson, savedGameStateHolder);
			asJson.put("relevantMemory", memoryJson);
		}

		if (interrupted) {
			asJson.put("interrupted", true);
		}

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.goal = relatedStores.goalDictionary.getByName(asJson.getString("goal"));
		if (this.goal == null) {
			throw new InvalidSaveException("Could not find goal with name " + asJson.getString("goal"));
		}

		JSONArray actionsJson = asJson.getJSONArray("actions");
		if (actionsJson != null) {
			for (int cursor = 0; cursor < actionsJson.size(); cursor++) {
				JSONObject actionJson = actionsJson.getJSONObject(cursor);
				Class<? extends Action> actionClass = relatedStores.actionDictionary.getByName(actionJson.getString("name"));
				if (actionClass == null) {
					throw new InvalidSaveException("Could not find action by name " + actionJson.getString("name"));
				}
				Action action = Action.newInstance(actionClass, this);
				action.readFrom(actionJson, savedGameStateHolder, relatedStores);
				this.actionQueue.add(action);
			}
		}

		Long assignedJobId = asJson.getLong("assignedJob");
		if (assignedJobId != null) {
			this.assignedJob = savedGameStateHolder.jobs.get(assignedJobId);
			if (this.assignedJob == null) {
				throw new InvalidSaveException("Could not find job by ID " + assignedJobId);
			}
		}

		Long haulingAllocationId = asJson.getLong("haulingAllocation");
		if (haulingAllocationId != null) {
			this.assignedHaulingAllocation = savedGameStateHolder.haulingAllocations.get(haulingAllocationId);
			if (this.assignedHaulingAllocation == null) {
				throw new InvalidSaveException("Could not find hauling allocation by ID " + haulingAllocationId);
			}
		}

		JSONObject foodAllocationJson = asJson.getJSONObject("foodAllocation");
		if (foodAllocationJson != null) {
			this.foodAllocation = new FoodAllocation();
			this.foodAllocation.readFrom(foodAllocationJson, savedGameStateHolder, relatedStores);
		}

		Long liquidAllocationId = asJson.getLong("liquidAllocation");
		if (liquidAllocationId != null) {
			this.liquidAllocation = savedGameStateHolder.liquidAllocations.get(liquidAllocationId);
			if (this.liquidAllocation == null) {
				throw new InvalidSaveException("Could not find liquid allocation with ID " + liquidAllocationId);
			}
		}

		this.assignedFurnitureId = asJson.getLong("assignedFurnitureId");

		JSONObject targetLocationJson = asJson.getJSONObject("targetLocation");
		if (targetLocationJson != null) {
			this.targetLocation = new Vector2(
					targetLocationJson.getFloatValue("x"),
					targetLocationJson.getFloatValue("y")
			);
		}

		JSONObject memoryJson = asJson.getJSONObject("relevantMemory");
		if (memoryJson != null) {
			this.relevantMemory = new Memory();
			this.relevantMemory.readFrom(memoryJson, savedGameStateHolder, relatedStores);
		}

		interrupted = asJson.getBooleanValue("interrupted");
	}

}
