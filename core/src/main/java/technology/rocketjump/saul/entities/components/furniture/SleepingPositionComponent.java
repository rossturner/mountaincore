package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class SleepingPositionComponent implements EntityComponent {

	private EntityAssetOrientation sleepingOrientation;
	private boolean isOnFloor;

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		SleepingPositionComponent cloned = new SleepingPositionComponent();
		cloned.setSleepingOrientation(this.sleepingOrientation);
		return cloned;
	}

	public EntityAssetOrientation getSleepingOrientation() {
		return sleepingOrientation;
	}

	public void setSleepingOrientation(EntityAssetOrientation sleepingOrientation) {
		this.sleepingOrientation = sleepingOrientation;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!EntityAssetOrientation.DOWN.equals(sleepingOrientation)) {
			asJson.put("orientation", sleepingOrientation.name());
		}

		if (isOnFloor) {
			asJson.put("isOnFloor", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.sleepingOrientation = EnumParser.getEnumValue(asJson, "orientation", EntityAssetOrientation.class, EntityAssetOrientation.DOWN);
		this.isOnFloor = asJson.getBooleanValue("isOnFloor");
	}

	public boolean isOnFloor() {
		return isOnFloor;
	}

	public void setOnFloor(boolean onFloor) {
		isOnFloor = onFloor;
	}
}
