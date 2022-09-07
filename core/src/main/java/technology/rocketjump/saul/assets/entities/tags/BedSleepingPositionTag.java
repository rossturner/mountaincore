package technology.rocketjump.saul.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;

public class BedSleepingPositionTag extends Tag {
	@Override
	public String getTagName() {
		return "BED_SLEEPING_POSITION";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return EnumUtils.isValidEnum(EntityAssetOrientation.class, args.get(0)) &&
				EnumUtils.isValidEnum(BedAssignment.class, args.get(1)) &&
				EnumUtils.isValidEnum(BedCreaturePosition.class, args.get(2));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		SleepingPositionComponent sleepingPositionComponent = entity.getOrCreateComponent(SleepingPositionComponent.class);
		sleepingPositionComponent.setSleepingOrientation(EntityAssetOrientation.valueOf(args.get(0)));
		sleepingPositionComponent.setAssignmentType(BedAssignment.valueOf(args.get(1)));
		sleepingPositionComponent.setBedCreaturePosition(BedCreaturePosition.valueOf(args.get(2)));
	}

	public enum BedAssignment {
		MILITARY_ONLY,
		CIVILIAN_ONLY
	}

	public enum BedCreaturePosition {
		INSIDE_FURNITURE,
		ON_GROUND
	}
}
