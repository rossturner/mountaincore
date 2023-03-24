package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

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
