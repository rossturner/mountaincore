package technology.rocketjump.saul.entities.ai.goap.actions;

import technology.rocketjump.saul.entities.model.physical.item.ItemType;

import java.util.List;
import java.util.Optional;

public interface ItemTypeLookupCallback {

	void itemTypeFound(Optional<ItemType> itemTypeLookup);

	void itemTypesFound(List<ItemType> itemTypes);

}
