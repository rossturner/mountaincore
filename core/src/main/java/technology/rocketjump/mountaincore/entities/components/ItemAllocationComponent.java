package technology.rocketjump.mountaincore.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemAllocationComponent implements InfrequentlyUpdatableComponent, Destructible {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private List<ItemAllocation> allocations = new ArrayList<>();

	public ItemAllocationComponent() {

	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
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
		double expiryGameTime = 0D;
		if (gameContext != null) {
			GameClock gameClock = gameContext.getGameClock();
			double hoursInDay = gameClock.HOURS_IN_DAY;
			double currentGameTime = gameClock.getCurrentGameTime();
			expiryGameTime = currentGameTime + hoursInDay;
		}

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
			ItemAllocation itemAllocation = new ItemAllocation(parentEntity, numToAllocate, requestingEntity, purpose, expiryGameTime);
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

	public ItemAllocation swapAllocationPurpose(ItemAllocation.Purpose existingPurpose, ItemAllocation.Purpose newPurpose, int quantity) {
		for (ItemAllocation existingAllocation : new ArrayList<>(this.allocations)) {
			if (existingAllocation.getPurpose().equals(existingPurpose) && existingAllocation.getAllocationAmount() >= quantity) {

				ItemAllocation newAllocation = existingAllocation.clone();
				newAllocation.setPurpose(newPurpose);
				newAllocation.setAllocationAmount(quantity);

				existingAllocation.setAllocationAmount(existingAllocation.getAllocationAmount() - quantity);
				if (existingAllocation.getAllocationAmount() == 0) {
					cancel(existingAllocation);
				}

				return newAllocation;
			}
		}

		Logger.error("Could not swap allocation purpose");
		return null;
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

	private List<ItemAllocation> getExpired() {
		return getAll().stream().filter(this::isExpired).toList();
	}

	private boolean isExpired(ItemAllocation itemAllocation) {
		if (gameContext == null) {
			return false;
		} else {
			double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
			double expiryGameTime = itemAllocation.getExpiryGameTime();
			return expiryGameTime < currentGameTime;
		}
	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		List<ItemAllocation> itemAllocationsToCancel = new ArrayList<>();


		for (ItemAllocation itemAllocation : getExpired()) {

			//find allocations for hauling without a job
			Long relatedHaulingAllocationId = itemAllocation.getRelatedHaulingAllocationId();
			if (relatedHaulingAllocationId != null) {
				long haulingJobCount = gameContext.getJobs()
						.values()
						.stream()
						.filter(j -> j.getHaulingAllocation() != null
								&& relatedHaulingAllocationId.equals(j.getHaulingAllocation().getHaulingAllocationId()))
						.count();


				if (haulingJobCount == 0) {
					itemAllocationsToCancel.add(itemAllocation);
				}
			}
		}

		for (ItemAllocation toCancel : itemAllocationsToCancel) {
			Logger.warn("Cancelling item allocation id={} {} as hauling job can no longer be found", toCancel.getItemAllocationId(), toCancel);
			messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, toCancel);
			allocations.remove(toCancel);
		}
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
