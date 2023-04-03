package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class ExtraMaterialTypesToColorMappingsTag extends Tag {

	@Override
	public String getTagName() {
		return "EXTRA_MATERIAL_TYPES_TO_COLOR_MAPPINGS";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		boolean correctNumberOfArgs = args.size() > 0 && args.size() % 2 == 0;
		if (correctNumberOfArgs) {
			for (int i = 0; i < args.size(); i+=2) {
				String materialTypeArg = args.get(i);
				String coloringLayerArg = args.get(i+1);
				if (!EnumUtils.isValidEnum(GameMaterialType.class, materialTypeArg)) {
					return false;
				}
				if (!EnumUtils.isValidEnum(ColoringLayer.class, coloringLayerArg)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {


		for (int i = 0; i < args.size(); i+=2) {
			String materialTypeArg = args.get(i);
			String coloringLayerArg = args.get(i+1);
			GameMaterialType materialType = GameMaterialType.valueOf(materialTypeArg);
			ColoringLayer coloringLayer = ColoringLayer.valueOf(coloringLayerArg);

			GameMaterial gameMaterial = selectMaterial(entity, materialType, tagProcessingUtils);

			setColoringLayer(entity.getPhysicalEntityComponent().getAttributes(), coloringLayer, gameMaterial);
		}
	}

	private GameMaterial selectMaterial(Entity entity, GameMaterialType materialType, TagProcessingUtils tagProcessingUtils) {
		Color existingMaterialColor = getExistingMateralColor(entity.getPhysicalEntityComponent().getAttributes(), materialType);

		Optional<GameMaterial> highestContrastingColor = tagProcessingUtils.materialDictionary
				.getByType(materialType)
				.stream()
				.filter(GameMaterial::isUseInRandomGeneration)
				.max(Comparator.comparing(a -> contrastRatio(getColor(a), existingMaterialColor)));

		return highestContrastingColor.orElse(null);
	}

	private Color getExistingMateralColor(EntityAttributes attributes, GameMaterialType materialType) {
		Map<GameMaterialType, GameMaterial> materials = attributes.getMaterials();
		if (materials != null && materials.containsKey(materialType)) {
			return getColor(materials.get(materialType));
		}
		return Color.WHITE;
	}

	private Color getColor(GameMaterial gameMaterial) {
		if (gameMaterial.getColor() == null) {
			return Color.WHITE;
		} else {
			return gameMaterial.getColor();
		}
	}

	private void setColoringLayer(EntityAttributes attributes, ColoringLayer coloringLayer, GameMaterial gameMaterial) {
		if (gameMaterial == null) {
			return;
		}
		if (attributes instanceof FurnitureEntityAttributes furnitureEntityAttributes) {
			furnitureEntityAttributes.setColor(coloringLayer, gameMaterial.getColor());
		} else if (attributes instanceof ItemEntityAttributes itemEntityAttributes) {
			itemEntityAttributes.setColor(coloringLayer, gameMaterial.getColor());
		} else {
			Logger.error("Not yet implemented: " + getTagName() + " for " + attributes.getClass());
		}
	}

	private float luminance(Color a) {
		return (0.2126f * a.r) + (0.7152f * a.g) + (0.0722f * a.b);
	}

	private float contrastRatio(Color a, Color b) {
		float luminanceA = luminance(a);
		float luminanceB = luminance(b);
		float c = 0.05f;
		if (luminanceA > luminanceB) {
			return (luminanceA + c) / (luminanceB + c);
		} else {
			return (luminanceB + c) / (luminanceA + c);
		}
	}
}
