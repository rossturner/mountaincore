package technology.rocketjump.mountaincore.mapping.model;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class ImpendingMiningCollapse implements ChildPersistable {

	private GridPoint2 epicenter;
	private double collapseGameTime;

	public ImpendingMiningCollapse(GridPoint2 epicenter, double collapseGameTime) {
		this.epicenter = epicenter;
		this.collapseGameTime = collapseGameTime;
	}

	public ImpendingMiningCollapse() {

	}

	public GridPoint2 getEpicenter() {
		return epicenter;
	}

	public double getCollapseGameTime() {
		return collapseGameTime;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("epicenter", JSONUtils.toJSON(epicenter));
		asJson.put("time", collapseGameTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.epicenter = JSONUtils.gridPoint2(asJson.getJSONObject("epicenter"));
		this.collapseGameTime = asJson.getDoubleValue("time");
	}
}
