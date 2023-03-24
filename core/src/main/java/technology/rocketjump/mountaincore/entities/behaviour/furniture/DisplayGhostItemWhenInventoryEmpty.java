package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;

public interface DisplayGhostItemWhenInventoryEmpty {

	ItemType getSelectedItemType();

	Color getOverrideColor();
}
