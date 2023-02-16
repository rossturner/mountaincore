package technology.rocketjump.saul.rooms;

import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.rooms.constructions.Construction;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.rooms.HaulingAllocation.AllocationPositionType.*;

public class HaulingAllocationBuilder {

	private final HaulingAllocation allocation = new HaulingAllocation();

	private HaulingAllocationBuilder() {

	}

	public static HaulingAllocationBuilder createWithItemAllocation(int quantity, Entity entityToHaul, Entity requestingEntity) {
		ItemAllocationComponent itemAllocationComponent = entityToHaul.getOrCreateComponent(ItemAllocationComponent.class);
		ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(quantity, requestingEntity, ItemAllocation.Purpose.DUE_TO_BE_HAULED);

		HaulingAllocationBuilder builder = new HaulingAllocationBuilder();
		builder.allocation.setItemAllocation(itemAllocation);
		builder.allocation.setHauledEntityId(entityToHaul.getId());
		builder.allocation.setHauledEntityType(entityToHaul.getType()); // Note that this is usually an ITEM but might be a CREATURE

		Entity containerEntity = entityToHaul.getLocationComponent().getContainerEntity();
		if (containerEntity != null) {
			return builder.fromContainer(containerEntity);
		} else {
			builder.allocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FLOOR);
			builder.allocation.setSourcePosition(toGridPoint(entityToHaul.getLocationComponent().getWorldPosition()));
			return builder;
		}
	}

	public static HaulingAllocationBuilder createToHaulFurniture(Entity furnitureToBeHauled) {
		if (furnitureToBeHauled.getType().equals(EntityType.FURNITURE)) {
			HaulingAllocationBuilder builder = new HaulingAllocationBuilder();
			builder.allocation.setHauledEntityType(EntityType.FURNITURE);
			builder.allocation.setHauledEntityId(furnitureToBeHauled.getId());
			builder.allocation.setSourcePositionType(FURNITURE);
			builder.allocation.setSourcePosition(toGridPoint(furnitureToBeHauled.getLocationComponent().getWorldPosition()));
			return builder;
		} else {
			throw new IllegalArgumentException(furnitureToBeHauled + " is not furniture");
		}
	}


	public static HaulingAllocationBuilder createToHaulCreature(Entity creatureEntity) {
		if (creatureEntity.getType().equals(EntityType.CREATURE)) {
			// FIXME Should this be set up with an item allocation?
			HaulingAllocationBuilder builder = new HaulingAllocationBuilder();
			builder.allocation.setHauledEntityType(EntityType.CREATURE);
			builder.allocation.setHauledEntityId(creatureEntity.getId());
			// assuming always collecting from floor - should probably extend to include from container
			builder.allocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FLOOR);
			builder.allocation.setSourcePosition(toGridPoint(creatureEntity.getLocationComponent().getWorldPosition()));
			return builder;
		} else {
			throw new IllegalArgumentException(creatureEntity + " is not a creature");
		}
	}

	public HaulingAllocationBuilder withLiquidAllocation(LiquidAllocation liquidAllocation) {
		allocation.setLiquidAllocation(liquidAllocation);
		return this;
	}

	private HaulingAllocationBuilder fromContainer(Entity containerEntity) {
		if (containerEntity.getType().equals(EntityType.FURNITURE)) {
			allocation.setSourceContainerId(containerEntity.getId());
			allocation.setSourcePositionType(FURNITURE);
			allocation.setSourcePosition(toGridPoint(containerEntity.getLocationComponent().getWorldPosition()));
			return this;
		} else if (containerEntity.getType().equals(EntityType.CREATURE)) {
			allocation.setSourceContainerId(containerEntity.getId());
			allocation.setSourcePositionType(CREATURE);
			return this;
		} else {
			throw new NotImplementedException("From container entity is not furniture");
		}
	}

	public HaulingAllocation toEntity(Entity targetEntity) {
		allocation.setTargetId(targetEntity.getId());
		allocation.setTargetPosition(toGridPoint(targetEntity.getLocationComponent().getWorldOrParentPosition()));

		// might be able to eliminate hauling to item
		switch (targetEntity.getType()) {
			case FURNITURE -> allocation.setTargetPositionType(FURNITURE);
			case CREATURE -> allocation.setTargetPositionType(CREATURE);
			case ITEM -> allocation.setTargetPositionType(FLOOR);
			default -> throw new NotImplementedException(targetEntity.getType() + " not implemented in " + getClass().getSimpleName() + ".toEntity()");
		}
		return build();
	}

	public HaulingAllocation toConstruction(Construction construction) {
		allocation.setTargetPosition(construction.getPrimaryLocation());
		allocation.setTargetId(construction.getId());
		allocation.setTargetPositionType(CONSTRUCTION);
		return build();
	}

	public HaulingAllocation toRoom(Room room, GridPoint2 location) {
		allocation.setTargetPosition(location);
		allocation.setTargetPositionType(ROOM);
		allocation.setTargetId(room.getRoomId());
		return build();
	}

	public HaulingAllocation toUnspecifiedLocation() {
		return build();
	}

	private HaulingAllocation build() {
		// TODO verify all properties are set correctly

		return allocation;
	}

}
