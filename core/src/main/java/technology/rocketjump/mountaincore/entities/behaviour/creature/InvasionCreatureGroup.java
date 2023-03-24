package technology.rocketjump.mountaincore.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion.CreateCampfireAction;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion.VictoryPointsForStealingAction;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.invasions.model.InvasionDefinition;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class InvasionCreatureGroup extends CreatureGroup {

	private static final int POINTS_LOST_ON_MEMBER_DEATH = 80;

	private InvasionDefinition invasionDefinition;
	private InvasionStage invasionStage = InvasionStage.ARRIVING;
	private double hoursInCurrentStage;
	private int victoryPointsEarned; // Used to track progress towards the goals of the invasion - killing settlers and stealing goods
	private int victoryPointsTarget;

	private SpecialGoal pendingSpecialGoal;

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		double now = gameContext.getGameClock().getCurrentGameTime();
		if (lastUpdateGameTime == 0) {
			lastUpdateGameTime = now;
		}
		double elapsed = now - lastUpdateGameTime;
		lastUpdateGameTime = now;
		hoursInCurrentStage += elapsed;

		if (invasionStage.equals(InvasionStage.RAIDING) && victoryPointsEarned >= victoryPointsTarget) {
			this.invasionStage = InvasionStage.RETREATING;
			this.hoursInCurrentStage = 0;
		}

		if (hoursInCurrentStage > invasionStage.durationHours) {
			switch (invasionStage) {
				case ARRIVING -> {
					this.pendingSpecialGoal = SpecialGoal.CREATE_CAMPFIRE;
					this.invasionStage = InvasionStage.PREPARING;
					this.hoursInCurrentStage = 0;
				}
				case PREPARING -> {
					removeCampfire(gameContext, messageDispatcher);
					this.invasionStage = InvasionStage.RAIDING;
					this.hoursInCurrentStage = 0;
				}
				case RAIDING -> {
					this.invasionStage = InvasionStage.RETREATING;
					this.hoursInCurrentStage = 0;
				}
				case RETREATING -> {
					// Do nothing
				}
			}
		}
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

	public void addVictoryPoints(int points) {
		this.victoryPointsEarned += points;
	}

	public SpecialGoal popSpecialGoal() {
		if (pendingSpecialGoal != null) {
			SpecialGoal temp = this.pendingSpecialGoal;
			this.pendingSpecialGoal = null;
			return temp;
		} else {
			return null;
		}
	}

	public void setInvasionDefinition(InvasionDefinition invasionDefinition) {
		this.invasionDefinition = invasionDefinition;
	}

	private void removeCampfire(GameContext gameContext, MessageDispatcher messageDispatcher) {
		gameContext.getAreaMap().getTile(homeLocation).getEntities()
				.stream().filter(e -> e.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes attributes &&
						attributes.getFurnitureType().getName().equals(CreateCampfireAction.CAMPFIRE_FURNITURE_TYPE_NAME))
				.findAny()
				.ifPresent(entity -> {
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entity);
				});
	}

	public InvasionStage getInvasionStage() {
		return invasionStage;
	}

	public void killedEnemy(Entity deceased) {
		if (deceased.getOrCreateComponent(FactionComponent.class).getFaction().equals(Faction.SETTLEMENT)) {
			addVictoryPoints(VictoryPointsForStealingAction.VICTORY_POINTS_FOR_KILLING_SETTLER);
		}
	}

	@Override
	public void removeMemberId(long entityId) {
		super.removeMemberId(entityId);
		this.victoryPointsTarget -= POINTS_LOST_ON_MEMBER_DEATH;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(savedGameStateHolder);

		JSONObject asJson = savedGameStateHolder.creatureGroupJson.getJSONObject(savedGameStateHolder.creatureGroupJson.size() - 1);
		if (asJson.getLongValue("groupId") == this.groupId) {
			asJson.put("_class", getClass().getName());

			asJson.put("invasionDefinition", invasionDefinition.getName());

			if (!invasionStage.equals(InvasionStage.ARRIVING)) {
				asJson.put("invasionStage", invasionStage.name());
			}
			asJson.put("hoursInCurrentStage", hoursInCurrentStage);

			if (pendingSpecialGoal != null) {
				asJson.put("pendingSpecialGoal", pendingSpecialGoal.name());
			}

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

		this.pendingSpecialGoal = EnumParser.getEnumValue(asJson, "pendingSpecialGoal", SpecialGoal.class, null);

		this.victoryPointsEarned = asJson.getIntValue("victoryPointsEarned");
		this.victoryPointsTarget = asJson.getIntValue("victoryPointsTarget");
	}
}
