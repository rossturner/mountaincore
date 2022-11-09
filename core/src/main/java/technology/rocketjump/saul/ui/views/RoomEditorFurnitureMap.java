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
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.*;

@Singleton
public class RoomEditorFurnitureMap {

	private final Map<FurnitureType, Entity> byType = new HashMap<>();
	private final GameMaterialDictionary materialDictionary;
	private final Random random;
	private Map<GameMaterialType, GameMaterial> selectedMaterials = new HashMap<>();
	private GameMaterialType selectedType = GameMaterialType.WOOD;

	@Inject
	public RoomEditorFurnitureMap(FurnitureEntityAttributesFactory furnitureEntityAttributesFactory,
								  FurnitureEntityFactory furnitureEntityFactory,
								  FurnitureTypeDictionary furnitureTypeDictionary,
								  GameMaterialDictionary materialDictionary) {
		this.materialDictionary = materialDictionary;
		random = new RandomXS128();

		for (GameMaterialType materialType : GameMaterialType.values()) {
			selectedMaterials.put(materialType, pickMaterial(materialType));
		}


		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			if (furnitureType.getRequirements() != null) {
				Set<GameMaterialType> applicableTypes = furnitureType.getRequirements().keySet();

				FurnitureEntityAttributes attributes = null;
				List<QuantifiedItemType> requirements = null;
				if (applicableTypes.contains(selectedType)) {
					requirements = furnitureType.getRequirements().get(selectedType);
					attributes = furnitureEntityAttributesFactory.byType(furnitureType, selectedMaterials.get(selectedType));
				} else if (!applicableTypes.isEmpty()) {
					GameMaterialType otherType = applicableTypes.iterator().next();
					requirements = furnitureType.getRequirements().get(otherType);
					attributes = furnitureEntityAttributesFactory.byType(furnitureType, selectedMaterials.get(otherType));
				}

				if (attributes != null && requirements != null) {
					for (QuantifiedItemType requirement : requirements) {
						if (!attributes.getMaterials().containsKey(requirement.getItemType().getPrimaryMaterialType())) {
							attributes.setMaterial(selectedMaterials.get(requirement.getItemType().getPrimaryMaterialType()));
						}
					}

					Entity entity = furnitureEntityFactory.create(attributes, new GridPoint2(), null, null);
					byType.put(furnitureType, entity);
				}
			}
		}
	}

	public GameMaterial getExampleMaterialFor(GameMaterialType materialType) {
		return selectedMaterials.get(materialType);
	}

	private GameMaterial pickMaterial(GameMaterialType materialType) {
		List<GameMaterial> applicable = materialDictionary.getByType(materialType)
				.stream().filter(GameMaterial::isUseInRandomGeneration)
				.toList();
		return applicable.get(random.nextInt(applicable.size()));
	}

	public Entity getByFurnitureType(FurnitureType furnitureType) {
		return byType.get(furnitureType);
	}

}
