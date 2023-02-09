package technology.rocketjump.saul.settlement.trading.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class TraderInfo implements ChildPersistable {

	private Double hostilityCooldownInHours; // after being attacked *by settlement*, waits a number of years before visiting again

	private Integer nextVisitDayOfYear; // when null, initialise to a day near the start of 2 seasons from now
	private Double hoursUntilTraderArrives;

	private final List<ItemEntityAttributes> requestedItemsForNextVisit = new ArrayList<>();

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
		for (ItemEntityAttributes attributes : requestedItemsForNextVisit) {
			JSONObject attributesJson = new JSONObject(true);
			attributes.writeTo(attributesJson, savedGameStateHolder);
			requestedItemsForNextVisitJson.add(attributesJson);
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
			ItemEntityAttributes attributes = new ItemEntityAttributes();
			attributes.readFrom(attributesJson, savedGameStateHolder, relatedStores);
			this.requestedItemsForNextVisit.add(attributes);
		}
	}
}
