package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.saul.entities.ai.goap.actions.FurnitureAssignmentCallback;
import technology.rocketjump.saul.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag.BedCreaturePosition.INSIDE_FURNITURE;
import static technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag.BedCreaturePosition.ON_GROUND;

public class FurnitureAssignmentRequest {

	public final Entity requestingEntity;
	public final Class<? extends Tag> requiredTag;
	public final Predicate<Entity> filter;
	public final Consumer<Entity> callback;

	public static FurnitureAssignmentRequest requestBed(Entity requestingEntity, boolean wantsToSleepOnFloor, FurnitureAssignmentCallback callback) {
		return new FurnitureAssignmentRequest(BedSleepingPositionTag.class, requestingEntity, entity -> {
					SleepingPositionComponent sleepingPositionComponent = entity.getComponent(SleepingPositionComponent.class);
					return sleepingPositionComponent.getBedCreaturePosition().equals(wantsToSleepOnFloor ? ON_GROUND : INSIDE_FURNITURE) &&
							sleepingPositionComponent.isApplicableTo(requestingEntity);
				}, callback::furnitureAssigned);
	}

	public FurnitureAssignmentRequest(Class<? extends Tag> requiredTag, Entity requestingEntity, Predicate<Entity> furnitureFilter, Consumer<Entity> callback) {
		this.requestingEntity = requestingEntity;
		this.callback = callback;
		this.requiredTag = requiredTag;
		this.filter = furnitureFilter;
	}
}
