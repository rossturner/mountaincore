package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.*;

@Singleton
public class RoomEditorFurnitureMap {

	private final Map<FurnitureType, Entity> byType = new HashMap<>();
	private Map<GameMaterialType, GameMaterial> selectedMaterials = new HashMap<>();
	private GameMaterialType selectedType = GameMaterialType.WOOD;

	@Inject
	public RoomEditorFurnitureMap(FurnitureEntityAttributesFactory furnitureEntityAttributesFactory,
								  FurnitureEntityFactory furnitureEntityFactory,
								  FurnitureTypeDictionary furnitureTypeDictionary,
								  GameMaterialDictionary materialDictionary) {
		Random random = new RandomXS128();

		for (GameMaterialType materialType : GameMaterialType.values()) {
			List<GameMaterial> applicable = materialDictionary.getByType(materialType)
					.stream().filter(GameMaterial::isUseInRandomGeneration)
					.toList();
			selectedMaterials.put(materialType, applicable.get(random.nextInt(applicable.size())));
		}


		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			if (furnitureType.getRequirements() != null) {
				Set<GameMaterialType> applicableTypes = furnitureType.getRequirements().keySet();

				FurnitureEntityAttributes attributes = null;
				if (applicableTypes.contains(selectedType)) {
					attributes = furnitureEntityAttributesFactory.byType(furnitureType, selectedMaterials.get(selectedType));
				} else if (!applicableTypes.isEmpty()) {
					GameMaterialType otherType = applicableTypes.iterator().next();
					attributes = furnitureEntityAttributesFactory.byType(furnitureType, selectedMaterials.get(otherType));
				}

				if (attributes != null) {
					Entity entity = furnitureEntityFactory.create(attributes, new GridPoint2(), null, null);
					byType.put(furnitureType, entity);
				}
			}
		}
	}

	public Entity getByFurnitureType(FurnitureType furnitureType) {
		return byType.get(furnitureType);
	}

}
