package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Singleton
public class RoomEditorFurnitureMap {

	private final Map<FurnitureType, Entity> byType = new HashMap<>();
	private final GameMaterialDictionary materialDictionary;
	private final Random random;

	@Inject
	public RoomEditorFurnitureMap(FurnitureEntityAttributesFactory furnitureEntityAttributesFactory,
								  FurnitureEntityFactory furnitureEntityFactory,
								  FurnitureTypeDictionary furnitureTypeDictionary,
								  GameMaterialDictionary materialDictionary,
								  MessageDispatcher messageDispatcher) {
		this.materialDictionary = materialDictionary;
		random = new RandomXS128();

		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			if (furnitureType.getRequirements() != null) {

				GameMaterialType initialMaterialType = furnitureType.getRequirements().keySet().iterator().next();
				List<QuantifiedItemType> requirements = furnitureType.getRequirements().get(initialMaterialType);
				FurnitureEntityAttributes attributes = furnitureEntityAttributesFactory.byType(furnitureType, materialDictionary.getExampleMaterial(initialMaterialType));

				if (attributes != null && requirements != null) {
					for (QuantifiedItemType requirement : requirements) {
						if (!attributes.getMaterials().containsKey(requirement.getItemType().getPrimaryMaterialType())) {
							attributes.setMaterial(materialDictionary.getExampleMaterial(requirement.getItemType().getPrimaryMaterialType()));
						}
					}

					Entity entity = furnitureEntityFactory.create(attributes, new GridPoint2(), null, null);
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
					for (EntityAsset asset : entity.getPhysicalEntityComponent().getTypeMap().values()) {
						SpriteDescriptor spriteDescriptor = asset.getSpriteDescriptors().get(EntityAssetOrientation.DOWN);
						if (spriteDescriptor != null && spriteDescriptor.getColoringLayer() != null) {
							ColoringLayer coloringLayer = spriteDescriptor.getColoringLayer();
							if (coloringLayer.getLinkedMaterialType() != null && !attributes.getMaterials().containsKey(coloringLayer.getLinkedMaterialType())) {
								attributes.setMaterial(materialDictionary.getExampleMaterial(coloringLayer.getLinkedMaterialType()));
							}
						}
					}

					byType.put(furnitureType, entity);
				}
			}
		}
	}

	public Entity getByFurnitureType(FurnitureType furnitureType) {
		return byType.get(furnitureType);
	}

}
