package technology.rocketjump.saul.settlement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ItemAvailabilityChecker {

	private final SettlementItemTracker settlementItemTracker;

	@Inject
	public ItemAvailabilityChecker(SettlementItemTracker settlementItemTracker) {
		this.settlementItemTracker = settlementItemTracker;
	}

	public List<GameMaterial> getAvailableMaterialsFor(ItemType itemType, int minQuantity) {
		Map<GameMaterial, Integer> availabilityPerMaterial = new HashMap<>();

		List<Entity> itemsByType = settlementItemTracker.getItemsByType(itemType, true);
		for (Entity itemEntity : itemsByType) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
			ItemAllocationComponent allocationComponent = itemEntity.getComponent(ItemAllocationComponent.class);

			int currentAmount = availabilityPerMaterial.getOrDefault(attributes.getPrimaryMaterial(), 0);
			currentAmount += allocationComponent.getNumUnallocated();
			availabilityPerMaterial.put(attributes.getPrimaryMaterial(), currentAmount);
		}

		return availabilityPerMaterial.entrySet().stream()
				.filter(e -> e.getValue() >= minQuantity)
				.map(Map.Entry::getKey)
				.toList();
	}

	public int getAmountAvailable(ItemType itemType, GameMaterial material) {
		int amount = 0;
		for (Entity itemEntity : settlementItemTracker.getItemsByType(itemType, true)) {
			if (material == null || ((ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial().equals(material)) {
				ItemAllocationComponent allocationComponent = itemEntity.getComponent(ItemAllocationComponent.class);
				amount += allocationComponent.getNumUnallocated();
			}
		}
		return amount;
	}


}
