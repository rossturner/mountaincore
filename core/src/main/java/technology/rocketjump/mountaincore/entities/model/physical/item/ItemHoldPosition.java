package technology.rocketjump.mountaincore.entities.model.physical.item;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;

import java.util.Arrays;
import java.util.List;

// MODDING move this to data-driven ID-based type
public enum ItemHoldPosition {

	UNSPECIFIED(EntityAssetType.UNSPECIFIED),

	RIGHT_HAND("ATTACHMENT_RIGHT_HAND"),
	LEFT_HAND("ATTACHMENT_LEFT_HAND"),
	IN_FRONT("ATTACHMENT_FRONT_OF_BODY"),
	ON_SHOULDER("ATTACHMENT_ON_RIGHT_SHOULDER"),

	WORKSPACE_1("ATTACHMENT_WORKSPACE_1", "VEHICLE_ATTACHMENT_INVENTORY_1"),
	WORKSPACE_2("ATTACHMENT_WORKSPACE_2", "VEHICLE_ATTACHMENT_INVENTORY_2"),
	WORKSPACE_3("ATTACHMENT_WORKSPACE_3", "VEHICLE_ATTACHMENT_INVENTORY_3"),
	WORKSPACE_4("ATTACHMENT_WORKSPACE_4"),
	WORKSPACE_5("ATTACHMENT_WORKSPACE_5"),
	WORKSPACE_6("ATTACHMENT_WORKSPACE_6"),
	WORKSPACE_7("ATTACHMENT_WORKSPACE_7"),
	WORKSPACE_8("ATTACHMENT_WORKSPACE_8"),

	DECORATION_1("ATTACHMENT_DECO_1"),
	DECORATION_2("ATTACHMENT_DECO_2"),
	DECORATION_3("ATTACHMENT_DECO_3"),

	VEHICLE_DRIVER(null, "VEHICLE_ATTACHMENT_DRIVER"),
	VEHICLE_DRAUGHT_ANIMAL(null, "VEHICLE_ATTACHMENT_DRAUGHT_ANIMAL");

	public static final List<ItemHoldPosition> FURNITURE_WORKSPACES = Arrays.asList(
			WORKSPACE_1, WORKSPACE_2, WORKSPACE_3, WORKSPACE_4,
			WORKSPACE_5, WORKSPACE_6, WORKSPACE_7, WORKSPACE_8
	);

	private final EntityAssetType attachmentType;
	private final EntityAssetType vehicleAttachmentType;

	ItemHoldPosition(String attachmentTypeName) {
		this.attachmentType = new EntityAssetType(attachmentTypeName);
		this.vehicleAttachmentType = null;
	}

	ItemHoldPosition(String attachmentTypeName, String alternateName) {
		this.attachmentType = new EntityAssetType(attachmentTypeName);
		this.vehicleAttachmentType = new EntityAssetType(alternateName);
	}

	public EntityAssetType getAttachmentType() {
		return attachmentType;
	}

	public EntityAssetType getVehicleAttachmentType() {
		return vehicleAttachmentType;
	}

}
