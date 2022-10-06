package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.function.Consumer;

public class FurnitureAttributesCreationRequestMessage {

	public final FurnitureType furnitureType;
	public final GameMaterialType gameMaterialType;
	public final Consumer<FurnitureEntityAttributes> callback;

	public FurnitureAttributesCreationRequestMessage(FurnitureType furnitureType, GameMaterialType gameMaterialType, Consumer<FurnitureEntityAttributes> callback) {
		this.furnitureType = furnitureType;
		this.gameMaterialType = gameMaterialType;
		this.callback = callback;
	}
}
