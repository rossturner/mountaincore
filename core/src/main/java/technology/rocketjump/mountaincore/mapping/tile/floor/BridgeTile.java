package technology.rocketjump.mountaincore.mapping.tile.floor;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Bridge;
import technology.rocketjump.mountaincore.sprites.model.BridgeTileLayout;

public class BridgeTile implements ChildPersistable {

	private BridgeTileLayout bridgeTileLayout = BridgeTileLayout.CENTRE;

	public BridgeTile() {

	}

	public boolean isNavigable(Bridge bridge) {
//		if (bridge.isBeingDeconstructed()) {
//			// Only unnavigable once deconstruction has started
//			if (bridge.getDeconstructionJob().getWorkDoneSoFar() > 0) {
//				return false;
//			}
//		}
		return bridgeTileLayout.isNavigable(bridge.getOrientation());
	}

	public BridgeTileLayout getBridgeTileLayout() {
		return bridgeTileLayout;
	}

	public void setBridgeTileLayout(BridgeTileLayout bridgeTileLayout) {
		this.bridgeTileLayout = bridgeTileLayout;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("layout", bridgeTileLayout.name());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.bridgeTileLayout = EnumParser.getEnumValue(asJson, "layout", BridgeTileLayout.class, BridgeTileLayout.CENTRE);
	}
}
