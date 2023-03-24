package technology.rocketjump.mountaincore.rooms.components.behaviour;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.components.RoomComponent;

public abstract class RoomBehaviourComponent extends RoomComponent {

	protected JobPriority priority = JobPriority.NORMAL;

	public RoomBehaviourComponent(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	public abstract void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher);

	public JobPriority getPriority() {
		return this.priority;
	}

	public void setPriority(JobPriority jobPriority) {
		this.priority = jobPriority;

		for (RoomTile roomTile : parent.getRoomTiles().values()) {
			if (roomTile.getTile().hasConstruction()) {
				roomTile.getTile().getConstruction().setPriority(jobPriority, messageDispatcher);
			}
			for (Entity entity : roomTile.getTile().getEntities()) {
				if (entity.getBehaviourComponent() instanceof Prioritisable) {
					((Prioritisable)entity.getBehaviourComponent()).setPriority(jobPriority);
				}
			}
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!priority.equals(JobPriority.NORMAL)) {
			asJson.put("priority", priority.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.priority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.NORMAL);
	}
}
