package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class SleepingPositionComponent implements EntityComponent {

	private EntityAssetOrientation sleepingOrientation;
	private BedSleepingPositionTag.BedAssignment assignmentType;
	private BedSleepingPositionTag.BedCreaturePosition bedCreaturePosition;

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		SleepingPositionComponent cloned = new SleepingPositionComponent();
		cloned.sleepingOrientation = this.sleepingOrientation;
		cloned.assignmentType = this.assignmentType;
		cloned.bedCreaturePosition = this.bedCreaturePosition;
		return cloned;
	}

	public boolean isApplicableTo(Entity entity) {
		MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.isInMilitary()) {
			return assignmentType.equals(BedSleepingPositionTag.BedAssignment.MILITARY_ONLY);
		} else {
			return assignmentType.equals(BedSleepingPositionTag.BedAssignment.CIVILIAN_ONLY);
		}
	}

	public EntityAssetOrientation getSleepingOrientation() {
		return sleepingOrientation;
	}

	public void setSleepingOrientation(EntityAssetOrientation sleepingOrientation) {
		this.sleepingOrientation = sleepingOrientation;
	}

	public BedSleepingPositionTag.BedAssignment getAssignmentType() {
		return assignmentType;
	}

	public void setAssignmentType(BedSleepingPositionTag.BedAssignment assignmentType) {
		this.assignmentType = assignmentType;
	}

	public BedSleepingPositionTag.BedCreaturePosition getBedCreaturePosition() {
		return bedCreaturePosition;
	}

	public void setBedCreaturePosition(BedSleepingPositionTag.BedCreaturePosition creaturePosition) {
		bedCreaturePosition = creaturePosition;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!EntityAssetOrientation.DOWN.equals(sleepingOrientation)) {
			asJson.put("orientation", sleepingOrientation.name());
		}

		if (!BedSleepingPositionTag.BedAssignment.CIVILIAN_ONLY.equals(assignmentType)) {
			asJson.put("assignmentType", assignmentType);
		}

		if (!BedSleepingPositionTag.BedCreaturePosition.INSIDE_FURNITURE.equals(bedCreaturePosition)) {
			asJson.put("bedCreaturePosition", bedCreaturePosition);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.sleepingOrientation = EnumParser.getEnumValue(asJson, "orientation", EntityAssetOrientation.class, EntityAssetOrientation.DOWN);
		this.assignmentType = EnumParser.getEnumValue(asJson, "assignmentType", BedSleepingPositionTag.BedAssignment.class, BedSleepingPositionTag.BedAssignment.CIVILIAN_ONLY);
		this.bedCreaturePosition = EnumParser.getEnumValue(asJson, "bedCreaturePosition", BedSleepingPositionTag.BedCreaturePosition.class, BedSleepingPositionTag.BedCreaturePosition.INSIDE_FURNITURE);
	}

}
