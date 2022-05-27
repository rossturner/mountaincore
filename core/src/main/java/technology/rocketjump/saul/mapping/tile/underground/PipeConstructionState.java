package technology.rocketjump.saul.mapping.tile.underground;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.rendering.utils.HexColors;

public enum PipeConstructionState {

	NONE(Color.CLEAR),
	READY_FOR_CONSTRUCTION(HexColors.get("#FFFFFF66")),
	PENDING_DECONSTRUCTION(HexColors.get("#EE332E66"));

	public final Color renderColor;

	PipeConstructionState(Color renderColor) {
		this.renderColor = renderColor;
	}
}
