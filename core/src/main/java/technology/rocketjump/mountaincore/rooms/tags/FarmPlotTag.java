package technology.rocketjump.mountaincore.rooms.tags;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.FarmPlotComponent;
import technology.rocketjump.mountaincore.rooms.components.behaviour.FarmPlotBehaviour;

public class FarmPlotTag extends Tag {
	@Override
	public String getTagName() {
		return "FARM_PLOT";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		// Should check that first arg is a valid floor type
		return args.size() == 5;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		FarmPlotBehaviour farmPlotBehaviour = room.createComponent(FarmPlotBehaviour.class, tagProcessingUtils.messageDispatcher);
		FarmPlotComponent farmPlotComponent = room.createComponent(FarmPlotComponent.class, tagProcessingUtils.messageDispatcher);

		FloorType specifiedFloorType = tagProcessingUtils.floorTypeDictionary.getByFloorTypeName(args.get(0));
		if (specifiedFloorType == null) {
			Logger.error("Unrecognised floor type " + args.get(0) + " in " + this.getTagName() + " tag");
		} else {
			farmPlotComponent.setFarmingFloorType(specifiedFloorType);
		}

		GameMaterial specifiedMaterial = tagProcessingUtils.materialDictionary.getByName(args.get(1));
		if (specifiedMaterial == null) {
			Logger.error("Unrecognised material " + args.get(1) + " in " + this.getTagName() + " tag");
		} else {
			farmPlotComponent.setFarmingFloorMaterial(specifiedMaterial);
		}

		farmPlotBehaviour.setJobTypes(
				tagProcessingUtils.jobTypeDictionary.getByName(args.get(2)),
				tagProcessingUtils.jobTypeDictionary.getByName(args.get(3)),
				tagProcessingUtils.jobTypeDictionary.getByName(args.get(4))
		);
	}
}
