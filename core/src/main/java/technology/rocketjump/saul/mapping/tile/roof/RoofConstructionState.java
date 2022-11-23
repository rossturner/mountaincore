package technology.rocketjump.saul.mapping.tile.roof;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import technology.rocketjump.saul.rendering.utils.HexColors;

public enum RoofConstructionState {

	NONE(Color.CLEAR, "preview_roof_icon"),
	PENDING(HexColors.get("#FFFF99AA"), "preview_roof_icon"),
	TOO_FAR_FROM_SUPPORT(HexColors.get("#B74E00AA"), "preview_no_roof_icon"),
	NO_ADJACENT_ROOF(HexColors.get("#d2d144AA"), "preview_roof_icon"),
	READY_FOR_CONSTRUCTION(HexColors.get("#FFFFFFAA"), "preview_roof_icon"),
	PENDING_DECONSTRUCTION(HexColors.get("#EE332EAA"), "preview_no_roof_icon");

	public final Color renderColor;
	public final String iconName;
	public Sprite icon;

	RoofConstructionState(Color renderColor, String iconName) {
		this.renderColor = renderColor;
		this.iconName = iconName;
	}
}
