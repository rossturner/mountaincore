package technology.rocketjump.saul.entities.model.physical;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemHoldPosition;

public class AttachedEntity {

	public final Entity entity;
	public final ItemHoldPosition holdPosition;

	private Color overrideRenderColor;

	public AttachedEntity(Entity entity, ItemHoldPosition holdPosition) {
		this.holdPosition = holdPosition;
		this.entity = entity;
	}

	public Color getOverrideRenderColor() {
		return overrideRenderColor;
	}

	public void setOverrideRenderColor(Color overrideRenderColor) {
		this.overrideRenderColor = overrideRenderColor;
	}
}
