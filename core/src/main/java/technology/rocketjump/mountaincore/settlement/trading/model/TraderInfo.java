package technology.rocketjump.mountaincore.settlement.trading.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class TraderInfo implements ChildPersistable {

	private Double hostilityCooldownInHours; // after being attacked *by settlement*, waits a number of years before visiting again

	private Integer nextVisitDayOfYear; // when null, initialise to a day near the start of next season from now (not winter)
	private Double hoursUntilTraderArrives;

	private final List<ItemTypeWithMaterial> requestedItemsForNextVisit = new ArrayList<>();

	public Double getHostilityCooldownInHours() {
		return hostilityCooldownInHours;
	}

	public void setHostilityCooldownInHours(Double hostilityCooldownInHours) {
		this.hostilityCooldownInHours = hostilityCooldownInHours;
	}

	public Integer getNextVisitDayOfYear() {
		return nextVisitDayOfYear;
	}

	public void setNextVisitDayOfYear(Integer nextVisitDayOfYear) {
		this.nextVisitDayOfYear = nextVisitDayOfYear;
	}

	public Double getHoursUntilTraderArrives() {
		return hoursUntilTraderArrives;
	}

	public void setHoursUntilTraderArrives(Double hoursUntilTraderArrives) {
		this.hoursUntilTraderArrives = hoursUntilTraderArrives;
	}

	public List<ItemTypeWithMaterial> getRequestedItemsForNextVisit() {
		return requestedItemsForNextVisit;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (hostilityCooldownInHours != null) {
			asJson.put("hostilityCooldownInHours", hostilityCooldownInHours);
		}

		if (nextVisitDayOfYear != null) {
			asJson.put("nextVisitDayOfYear", nextVisitDayOfYear);
		}

		if (hoursUntilTraderArrives != null) {
			asJson.put("hoursUntilTraderArrives", hoursUntilTraderArrives);
		}

		JSONArray requestedItemsForNextVisitJson = new JSONArray();
		for (ItemTypeWithMaterial itemTypeWithMaterial : requestedItemsForNextVisit) {
			JSONObject itemJson = new JSONObject(true);
			itemTypeWithMaterial.writeTo(itemJson, savedGameStateHolder);
			requestedItemsForNextVisitJson.add(itemJson);
		}
		asJson.put("requestedItemsForNextVisit", requestedItemsForNextVisitJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.hostilityCooldownInHours = asJson.getDouble("hostilityCooldownInHours");
		this.nextVisitDayOfYear = asJson.getInteger("nextVisitDayOfYear");
		this.hoursUntilTraderArrives = asJson.getDouble("hoursUntilTraderArrives");

		JSONArray requestedItemsForNextVisitJson = asJson.getJSONArray("requestedItemsForNextVisit");
		for (int index = 0; index < requestedItemsForNextVisitJson.size(); index++) {
			JSONObject attributesJson = requestedItemsForNextVisitJson.getJSONObject(index);
			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
			itemTypeWithMaterial.readFrom(attributesJson, savedGameStateHolder, relatedStores);
			this.requestedItemsForNextVisit.add(itemTypeWithMaterial);
		}
	}
}
