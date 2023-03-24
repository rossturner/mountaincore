package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.entities.components.furniture.DeceasedContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class DeceasedContainerTag extends Tag {

	@Override
	public String getTagName() {
		return "DECEASED_CONTAINER";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		if (args.isEmpty()) {
			return true;
		}

		boolean validTransformationType = tagProcessingUtils.furnitureTypeDictionary.getByName(args.get(0)) != null;

		boolean validQuantity = true;
		if (args.size() > 1) {
			validQuantity = StringUtils.isNumeric(args.get(1));
		}

		boolean validRaces = true;
		if (args.size() > 2) {
			for (int i = 2; i < args.size(); i++) {
				String arg = args.get(i);
				if (arg.startsWith("RACE_")) {
					String raceName = arg.replaceFirst("RACE_", "");
					Race race = tagProcessingUtils.raceDictionary.getByName(raceName);
					validRaces = validRaces && race != null;
				}
			}
		}

		return validTransformationType && validQuantity && validRaces;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getComponent(DeceasedContainerComponent.class) == null) {
			entity.addComponent(new DeceasedContainerComponent());
		}
	}

	public int getMaxCapacity() {
		if (args.size() > 1) {
			return Integer.parseInt(args.get(1));
		}
		return 1;
	}

    public boolean matchesRace(Race deceasedRace, Race settlerRace) {
		if (args.size() > 2) {
			for (int i = 2; i < args.size(); i++) {
				String arg = args.get(i);
				if (arg.startsWith("RACE_")) {
					String raceName = arg.replaceFirst("RACE_", "");
					if (raceName.equals(deceasedRace.getName())) {
						return true;
					}
				}
			}
			return false;
		} else {
			return settlerRace.equals(deceasedRace); //implicit, no races defined, defaults to settlers
		}
    }
}