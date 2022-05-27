package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;

public class MaterialSelectionMessage {

	public final GameMaterialType selectedMaterialType;
	public final GameMaterial selectedMaterial;
	public final ItemType resourceItemType;

	public MaterialSelectionMessage(GameMaterialType selectedMaterialType, GameMaterial selectedMaterial, ItemType resourceItemType) {
		this.selectedMaterialType = selectedMaterialType;
		this.selectedMaterial = selectedMaterial;
		this.resourceItemType = resourceItemType;
	}

}
