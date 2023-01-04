package technology.rocketjump.saul.entities.behaviour.furniture;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;

public interface DisplayGhostItemWhenInventoryEmpty {

	ItemType getSelectedItemType();

	Color getOverrideColor();
}
