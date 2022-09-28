package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.SpecialGoal;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.invasions.model.InvasionDefinition;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class InvasionCreatureGroup extends CreatureGroup {

	private InvasionDefinition invasionDefinition;
	private InvasionStage invasionStage = InvasionStage.ARRIVING;
	private double hoursInCurrentStage;
	private int victoryPointsEarned; // Used to track progress towards the goals of the invasion - killing settlers and stealing goods
	private int victoryPointsTarget;

	private SpecialGoal pendingSpecialGoal;

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double now = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = now - lastUpdateGameTime;
		lastUpdateGameTime = now;

		hoursInCurrentStage += elapsed;

		if (hoursInCurrentStage > invasionStage.durationHours) {
			switch (invasionStage) {
				case ARRIVING -> {

				}
				case PREPARING -> {

				}
				case RAIDING -> {

				}
				case RETREATING -> {
					// Do nothing
				}
			}
		}


		// TODO if invasionStage is arriving, get someone to set up camp

		// TODO at end of preparing, destroy all campfires
	}

	public int getVictoryPointsTarget() {
		return victoryPointsTarget;
	}

	public void setVictoryPointsTarget(int victoryPointsTarget) {
		this.victoryPointsTarget = victoryPointsTarget;
	}

	public InvasionDefinition getInvasionDefinition() {
		return invasionDefinition;
	}

	public void setInvasionDefinition(InvasionDefinition invasionDefinition) {
		this.invasionDefinition = invasionDefinition;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(savedGameStateHolder);

		JSONObject asJson = savedGameStateHolder.creatureGroupJson.getJSONObject(savedGameStateHolder.creatureGroupJson.size() - 1);
		if (asJson.getLongValue("groupId") == this.groupId) {
			asJson.put("invasionDefinition", invasionDefinition.getName());

			if (!invasionStage.equals(InvasionStage.ARRIVING)) {
				asJson.put("invasionStage", invasionStage.name());
			}
			asJson.put("hoursInCurrentStage", hoursInCurrentStage);

			asJson.put("victoryPointsEarned", victoryPointsEarned);
			asJson.put("victoryPointsTarget", victoryPointsTarget);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.invasionDefinition = relatedStores.invasionDefinitionDictionary.getByName(asJson.getString("invasionDefinition"));
		if (this.invasionDefinition == null) {
			throw new InvalidSaveException("Could not find invasion definition with name " + asJson.getString("invasionDefinition"));
		}

		this.invasionStage = EnumParser.getEnumValue(asJson, "invasionStage", InvasionStage.class, InvasionStage.ARRIVING);
		this.hoursInCurrentStage = asJson.getDoubleValue("hoursInCurrentStage");

		this.victoryPointsEarned = asJson.getIntValue("victoryPointsEarned");
		this.victoryPointsTarget = asJson.getIntValue("victoryPointsTarget");
	}

}
