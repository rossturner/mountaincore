package technology.rocketjump.mountaincore.entities.ai.goap;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

public class PlannedTrade implements ChildPersistable {

	private long plannedTradeId = SequentialIdGenerator.nextId();
	private HaulingAllocation haulingAllocation;
	private ItemAllocation paymentItemAllocation;
	private Entity importExportFurniture;

	private transient Long importExportFurnitureId;

	public void init(GameContext gameContext) {
		if (importExportFurnitureId != null) {
			importExportFurniture = gameContext.getEntity(importExportFurnitureId);
			if (importExportFurniture == null) {
				throw new RuntimeException("Could not find importExportFurniture with ID " + importExportFurnitureId);
			} else {
				importExportFurnitureId = null;
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PlannedTrade that = (PlannedTrade) o;
		return plannedTradeId == that.plannedTradeId;
	}

	@Override
	public int hashCode() {
		return (int) (plannedTradeId ^ (plannedTradeId >>> 32));
	}


	public void setHaulingAllocation(HaulingAllocation haulingAllocation) {
		this.haulingAllocation = haulingAllocation;
	}

	public HaulingAllocation getHaulingAllocation() {
		return haulingAllocation;
	}


	public void setPaymentItemAllocation(ItemAllocation paymentItemAllocation) {
		this.paymentItemAllocation = paymentItemAllocation;
	}

	public ItemAllocation getPaymentItemAllocation() {
		return paymentItemAllocation;
	}

	public void setImportExportFurniture(Entity importExportFurniture) {
		this.importExportFurniture = importExportFurniture;
	}

	public Entity getImportExportFurniture() {
		return importExportFurniture;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("plannedTradeId", plannedTradeId);

		if (haulingAllocation != null) {
			haulingAllocation.writeTo(savedGameStateHolder);
			asJson.put("haulingAllocation", haulingAllocation.getHaulingAllocationId());
		}

		if (paymentItemAllocation != null) {
			paymentItemAllocation.writeTo(savedGameStateHolder);
			asJson.put("paymentItemAllocation", paymentItemAllocation.getItemAllocationId());
		}

		if (importExportFurniture != null) {
			importExportFurniture.writeTo(savedGameStateHolder);
			asJson.put("importExportFurniture", importExportFurniture.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		plannedTradeId = asJson.getLongValue("plannedTradeId");

		Long haulingAllocationId = asJson.getLong("haulingAllocation");
		if (haulingAllocationId != null) {
			this.haulingAllocation = savedGameStateHolder.haulingAllocations.get(haulingAllocationId);
			if (this.haulingAllocation == null) {
				throw new InvalidSaveException("Could not find hauling allocation by ID " + haulingAllocationId);
			}
		}

		Long paymentItemAllocationId = asJson.getLong("paymentItemAllocation");
		if (paymentItemAllocationId != null) {
			this.paymentItemAllocation = savedGameStateHolder.itemAllocations.get(paymentItemAllocationId);
			if (this.paymentItemAllocation == null) {
				throw new InvalidSaveException("Could not find item allocation by ID " + paymentItemAllocationId);
			}
		}

		this.importExportFurnitureId = asJson.getLong("importExportFurniture");
	}

}
