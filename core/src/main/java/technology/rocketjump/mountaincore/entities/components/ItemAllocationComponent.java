package technology.rocketjump.mountaincore.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemAllocationComponent implements ParentDependentEntityComponent, Destructible {

	private Entity parentEntity;

	private List<ItemAllocation> allocations = new ArrayList<>();

	public ItemAllocationComponent() {

	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ItemAllocationComponent other = new ItemAllocationComponent();
		for (ItemAllocation allocation : allocations) {
			other.allocations.add(allocation.clone());
		}
		return other;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		cancelAll();
	}

	public ItemAllocation createAllocation(int numToAllocate, Entity requestingEntity, ItemAllocation.Purpose purpose) {
		int quantity = 1;
		if (parentEntity.getType().equals(EntityType.ITEM)) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			quantity = attributes.getQuantity();
		}
		int currentAllocated = this.getNumAllocated();
		if (currentAllocated + numToAllocate > quantity) {
			String currentAllocationString = getAll().stream()
					.map(ItemAllocation::toString)
					.collect(Collectors.joining());
			throw new RuntimeException(String.format("Attempting to requestAllocation too many items. numToAllocate=%s quantity=%s currentAllocations=%s", numToAllocate, quantity, currentAllocationString));
		} else {
			ItemAllocation itemAllocation = new ItemAllocation(parentEntity, numToAllocate, requestingEntity, purpose);
			allocations.add(itemAllocation);
			return itemAllocation;
		}
	}

	public ItemAllocation cancel(ItemAllocation itemAllocation) {
		if (allocations.contains(itemAllocation) && !itemAllocation.isCancelled()) {
			allocations.remove(itemAllocation);
			itemAllocation.markAsCancelled();
			return itemAllocation;
		} else {
			Logger.error("Incorrect cancellation of {} id={}", this.getClass().getSimpleName(), itemAllocation.getItemAllocationId());
			return null;
		}
	}

	public void cancelAll(ItemAllocation.Purpose purposeToCancel) {
		for (ItemAllocation allocation : new ArrayList<>(allocations)) {
			if (allocation.getPurpose().equals(purposeToCancel)) {
				cancel(allocation);
			}
		}
	}

	public void cancelAll() {
		for (ItemAllocation allocation : new ArrayList<>(allocations)) {
			cancel(allocation);
		}
	}

	public ItemAllocation swapAllocationPurpose(ItemAllocation.Purpose existingPurpose, ItemAllocation.Purpose newPurpose, ItemAllocation itemAllocation) {
		int quantity = itemAllocation.getAllocationAmount();

		Optional<ItemAllocation> byId = this.allocations.stream()
				.filter(a -> Objects.equals(a.getItemAllocationId(), itemAllocation.getItemAllocationId()))
				.findAny();
		Optional<ItemAllocation> byPurposeAndQuantity = this.allocations.stream()
				.filter(a -> a.getPurpose().equals(existingPurpose) && a.getAllocationAmount() >= quantity)
				.findFirst();

		final ItemAllocation existingAllocation;
		if (byId.isPresent()) {
			existingAllocation = byId.get();
		} else if (byPurposeAndQuantity.isPresent()) {
			existingAllocation = byPurposeAndQuantity.get();
		} else {
			Logger.error("Could not swap allocation purpose");
			return null;
		}

		ItemAllocation newAllocation = existingAllocation.clone();
		newAllocation.setPurpose(newPurpose);
		newAllocation.setAllocationAmount(quantity);

		existingAllocation.setAllocationAmount(existingAllocation.getAllocationAmount() - quantity);
		if (existingAllocation.getAllocationAmount() == 0) {
			cancel(existingAllocation);
		}
		allocations.add(newAllocation);

		return newAllocation;

	}

	public ItemAllocation getAllocationForPurpose(ItemAllocation.Purpose requiredPurpose) {
		for (ItemAllocation allocation : allocations) {
			if (allocation.getPurpose().equals(requiredPurpose)) {
				return allocation;
			}
		}
		return null;
	}

	public int getNumAllocated() {
		int total = 0;
		for (ItemAllocation itemAllocation : allocations) {
			total += itemAllocation.getAllocationAmount();
		}
		return total;
	}

	public int getNumUnallocated() {
		int quantity = 1;
		if (parentEntity.getType().equals(EntityType.ITEM)) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			quantity = attributes.getQuantity();
		}
		return quantity - getNumAllocated();
	}


	public List<ItemAllocation> getAll() {
		return this.allocations;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!allocations.isEmpty()) {
			JSONArray allocationsArray = new JSONArray();
			for (ItemAllocation allocation : allocations) {
				allocation.writeTo(savedGameStateHolder);
				allocationsArray.add(allocation.getItemAllocationId());
			}
			asJson.put("allocations", allocationsArray);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray allocationsArray = asJson.getJSONArray("allocations");
		if (allocationsArray != null) {
			for (int cursor = 0; cursor < allocationsArray.size(); cursor++) {
				long allocationId = allocationsArray.getLongValue(cursor);
				ItemAllocation itemAllocation = savedGameStateHolder.itemAllocations.get(allocationId);
				if (itemAllocation == null) {
					throw new InvalidSaveException("Could not find item allocation with ID " + allocationId);
				} else {
					this.allocations.add(itemAllocation);
				}
			}
		}
	}

}
