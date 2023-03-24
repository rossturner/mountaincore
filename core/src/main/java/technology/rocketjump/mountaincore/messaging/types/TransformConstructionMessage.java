package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;

public class TransformConstructionMessage {

	public final Construction construction;
	public final FurnitureType transformToFurnitureType;

	public TransformConstructionMessage(Construction construction, FurnitureType transformToFurnitureType) {
		this.construction = construction;
		this.transformToFurnitureType = transformToFurnitureType;
	}

}
