package technology.rocketjump.saul.entities.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class AnimationComponent implements EntityComponent {
	public static final String WORK_ON_JOB = "work_on_job";
	private String currentAnimation;


	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		AnimationComponent clone = new AnimationComponent();
		clone.setCurrentAnimation(currentAnimation);
		return clone;
	}

	public String getCurrentAnimation() {
		return currentAnimation;
	}

	public void setCurrentAnimation(String currentAnimation) {
		this.currentAnimation = currentAnimation;
	}

	public void clearCurrentAnimation() {
		setCurrentAnimation(null);
	}


	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("currentAnimation", currentAnimation);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.currentAnimation = asJson.getString("currentAnimation");
	}

}
