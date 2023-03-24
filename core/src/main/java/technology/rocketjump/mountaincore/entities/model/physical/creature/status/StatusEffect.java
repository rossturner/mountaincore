package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.StatusMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public abstract class StatusEffect implements ChildPersistable {

	protected Entity parentEntity;
	protected double timeApplied = 0;

	protected final Class<? extends StatusEffect> nextStage;
	protected final Double hoursUntilNextStage;
	protected final DeathReason deathReason; // Only used if this statuseffect applies death
	protected Entity inflictedBy;

	protected StatusEffect(Class<? extends StatusEffect> nextStage, Double hoursUntilNextStage, DeathReason deathReason, Entity inflictedBy) {
		this.nextStage = nextStage;
		this.hoursUntilNextStage = hoursUntilNextStage;
		this.deathReason = deathReason;
		this.inflictedBy = inflictedBy;
	}

	public void infrequentUpdate(double elapsedTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		timeApplied += elapsedTime;

		if (checkForRemoval(gameContext)) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_STATUS, new StatusMessage(parentEntity, this.getClass(), null, inflictedBy));
		} else if (hoursUntilNextStage != null && timeApplied > hoursUntilNextStage) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_STATUS, new StatusMessage(parentEntity, this.getClass(), null, inflictedBy));
			if (nextStage != null) {
				messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, nextStage, deathReason, inflictedBy));
			}
		} else {
			applyOngoingEffect(gameContext, messageDispatcher);
		}
	}


	public abstract void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher);

	public abstract boolean checkForRemoval(GameContext gameContext);

	public abstract String getI18Key();

	public void onRemoval(GameContext gameContext, MessageDispatcher messageDispatcher) {

	}

	public void setParentEntity(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	transient boolean writtenInflictedBy;
	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("applied", timeApplied);

		if (inflictedBy != null) {
			if (!writtenInflictedBy) {
				writtenInflictedBy = true;
				inflictedBy.writeTo(savedGameStateHolder);
			}
			asJson.put("inflictedBy", inflictedBy.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.timeApplied = asJson.getDoubleValue("applied");

		Long inflictedById = asJson.getLong("inflictedBy");
		if (inflictedById != null) {
			this.inflictedBy = savedGameStateHolder.entities.get(inflictedById);
		}
	}
}
