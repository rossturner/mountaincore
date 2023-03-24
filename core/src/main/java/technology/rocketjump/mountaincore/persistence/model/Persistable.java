package technology.rocketjump.mountaincore.persistence.model;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;

public interface Persistable {

	void writeTo(SavedGameStateHolder savedGameStateHolder);

	void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException;

}
