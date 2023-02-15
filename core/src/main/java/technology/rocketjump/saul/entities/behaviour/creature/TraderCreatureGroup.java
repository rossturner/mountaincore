package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.ai.goap.SpecialGoal;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.behaviour.creature.TraderGroupStage.*;

public class TraderCreatureGroup extends CreatureGroup {

	private TraderGroupStage stage = SPAWNED;
	private SpecialGoal pendingSpecialGoal;
	private double hoursInCurrentStage;

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		double now = gameContext.getGameClock().getCurrentGameTime();
		if (lastUpdateGameTime == 0) {
			lastUpdateGameTime = now;
		}
		double elapsed = now - lastUpdateGameTime;
		lastUpdateGameTime = now;
		hoursInCurrentStage += elapsed;

		// TODO leave after spending too long in current stage

		switch (stage) {
			case SPAWNED -> {
				this.pendingSpecialGoal = SpecialGoal.MOVE_GROUP_TOWARDS_SETTLEMENT;
				this.stage = TraderGroupStage.ARRIVING;
				this.hoursInCurrentStage = 0;
			}

		}
	}

	public void progressToNextStage() {
		if (stage.equals(ARRIVING)) {
			this.stage = TraderGroupStage.MOVING_TO_TRADE_DEPOT;
			this.hoursInCurrentStage = 0;
			this.pendingSpecialGoal = SpecialGoal.MOVE_GROUP_TO_TRADE_DEPOT;
		} else if (stage.equals(MOVING_TO_TRADE_DEPOT)) {
			this.stage = TraderGroupStage.ARRIVED_AT_TRADE_DEPOT;
			this.hoursInCurrentStage = 0;
		}
	}

	public SpecialGoal popSpecialGoal() {
		if (pendingSpecialGoal != null) {
			SpecialGoal temp = this.pendingSpecialGoal;
			this.pendingSpecialGoal = null;
			return temp;
		} else {
			return null;
		}
	}

	public TraderGroupStage getStage() {
		return stage;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(savedGameStateHolder);

		JSONObject asJson = savedGameStateHolder.creatureGroupJson.getJSONObject(savedGameStateHolder.creatureGroupJson.size() - 1);
		if (asJson.getLongValue("groupId") == this.groupId) {
			asJson.put("_class", getClass().getName());

			asJson.put("stage", stage.name());
			asJson.put("hoursInCurrentStage", hoursInCurrentStage);

			if (pendingSpecialGoal != null) {
				asJson.put("pendingSpecialGoal", pendingSpecialGoal.name());
			}
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.stage = EnumParser.getEnumValue(asJson, "stage", TraderGroupStage.class, TraderGroupStage.ARRIVING);
		this.hoursInCurrentStage = asJson.getDoubleValue("hoursInCurrentStage");

		this.pendingSpecialGoal = EnumParser.getEnumValue(asJson, "pendingSpecialGoal", SpecialGoal.class, null);
	}
}
