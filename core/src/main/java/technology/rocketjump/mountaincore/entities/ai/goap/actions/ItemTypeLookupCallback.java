package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;

import java.util.List;
import java.util.Optional;

public interface ItemTypeLookupCallback {

	void itemTypeFound(Optional<ItemType> itemTypeLookup);

	void itemTypesFound(List<ItemType> itemTypes);

}
