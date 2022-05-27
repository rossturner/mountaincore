package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.rooms.constructions.Construction;

public class TransformConstructionMessage {

	public final Construction construction;
	public final FurnitureType transformToFurnitureType;

	public TransformConstructionMessage(Construction construction, FurnitureType transformToFurnitureType) {
		this.construction = construction;
		this.transformToFurnitureType = transformToFurnitureType;
	}

}
