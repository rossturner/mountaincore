package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.ai.goap.actions.FurnitureAssignmentCallback;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;

public class FurnitureAssignmentRequest {

	public final Entity requestingEntity;
	public final FurnitureAssignmentCallback callback;
	public final Class<? extends Tag> requiredTag;
	public final boolean wantsToSleepOnFloor;

	public FurnitureAssignmentRequest(Class<? extends Tag> requiredTag, Entity requestingEntity, FurnitureAssignmentCallback callback, boolean wantsToSleepOnFloor) {
		this.requestingEntity = requestingEntity;
		this.callback = callback;
		this.requiredTag = requiredTag;
		this.wantsToSleepOnFloor = wantsToSleepOnFloor;
	}
}
