package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class SpecificExtraMaterialsTag extends Tag {

	@Override
	public String getTagName() {
		return "SPECIFIC_EXTRA_MATERIALS";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		switch (entity.getType()) {
			case FURNITURE: {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				for (String arg : args) {
					GameMaterial material = tagProcessingUtils.materialDictionary.getByName(arg);
					if (material == null) {
						Logger.error("Could not find material by name " + arg + " in " + getClass().getSimpleName());
					} else {
						attributes.setMaterial(material);
					}
				}
				break;
			}
			default:
				Logger.error("Not yet implemented: " + getTagName() + " for " + entity.getType());
		}
	}

}
