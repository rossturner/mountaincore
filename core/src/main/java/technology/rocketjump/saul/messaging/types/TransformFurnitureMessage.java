package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;

public class TransformFurnitureMessage {

	public final Entity furnitureEntity;
	public final FurnitureType transformToFurnitureType;

	public TransformFurnitureMessage(Entity furnitureEntity, FurnitureType transformToFurnitureType) {
		this.furnitureEntity = furnitureEntity;
		this.transformToFurnitureType = transformToFurnitureType;
	}

}
