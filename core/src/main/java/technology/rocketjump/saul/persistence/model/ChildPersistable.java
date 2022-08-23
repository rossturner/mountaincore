package technology.rocketjump.saul.persistence.model;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;

public interface ChildPersistable {

	default void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		throw new NotImplementedException("Implement this in " + this.getClass().getSimpleName());
	}

	default void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		throw new NotImplementedException("Implement this in " + this.getClass().getSimpleName());
	}

}
