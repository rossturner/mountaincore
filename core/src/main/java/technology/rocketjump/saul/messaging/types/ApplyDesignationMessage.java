package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.designation.Designation;
import technology.rocketjump.saul.ui.GameInteractionMode;

public class ApplyDesignationMessage {

	private final MapTile targetTile;
	private final Designation designationToApply;
	private final GameInteractionMode interactionMode;

	public ApplyDesignationMessage(MapTile targetTile, Designation designationToApply, GameInteractionMode interactionMode) {
		this.targetTile = targetTile;
		this.designationToApply = designationToApply;
		this.interactionMode = interactionMode;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

	public Designation getDesignationToApply() {
		return designationToApply;
	}

	public GameInteractionMode getInteractionMode() {
		return interactionMode;
	}
}
