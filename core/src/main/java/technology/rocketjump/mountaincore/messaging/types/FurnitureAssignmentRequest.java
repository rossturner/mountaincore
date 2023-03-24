package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.FurnitureAssignmentCallback;
import technology.rocketjump.mountaincore.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.tags.Tag;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class FurnitureAssignmentRequest {

	public final Entity requestingEntity;
	public final Class<? extends Tag> requiredTag;
	public final Predicate<Entity> filter;
	public final Consumer<Entity> callback;

	public static FurnitureAssignmentRequest requestBed(Entity requestingEntity, boolean wantsToSleepOnFloor, FurnitureAssignmentCallback callback) {
		return new FurnitureAssignmentRequest(BedSleepingPositionTag.class, requestingEntity, entity -> {
					SleepingPositionComponent sleepingPositionComponent = entity.getComponent(SleepingPositionComponent.class);
					return sleepingPositionComponent.getBedCreaturePosition().equals(wantsToSleepOnFloor ? BedSleepingPositionTag.BedCreaturePosition.ON_GROUND : BedSleepingPositionTag.BedCreaturePosition.INSIDE_FURNITURE) &&
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
