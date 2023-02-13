package technology.rocketjump.saul.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SelectOpponentAction extends Action {

	public SelectOpponentAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		MilitaryComponent militaryComponent = parent.parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.isInMilitary() && militaryComponent.getSquadId() != null) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null) {
				Set<Long> opponentIds = squad.getAttackEntityIds();

				if (!opponentIds.isEmpty()) {
					CombatStateComponent combatStateComponent = parent.parentEntity.getComponent(CombatStateComponent.class);
					combatStateComponent.setOpponentEntityIds(opponentIds);

					Optional<Entity> nearest = opponentIds.stream()
							.map(id -> gameContext.getEntities().get(id))
							.filter(Objects::nonNull)
							.filter(e -> {
								if (e.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
									return !creatureEntityAttributes.getConsciousness().equals(Consciousness.DEAD);
								} else {
									return true;
								}
							})
							.min((o1, o2) -> {
								float o1Distance = parent.parentEntity.getLocationComponent(true).getWorldOrParentPosition().dst2(
										o1.getLocationComponent(true).getWorldOrParentPosition());
								float o2Distance = parent.parentEntity.getLocationComponent(true).getWorldOrParentPosition().dst2(
										o2.getLocationComponent(true).getWorldOrParentPosition()
								);
								return (int) ((o1Distance - o2Distance) * 1000f);
							});

					if (nearest.isPresent()) {
						combatStateComponent.setTargetedOpponentId(nearest.get().getId());
						completionType = CompletionType.SUCCESS;
						return;
					}
				}

			}
		}
		completionType = CompletionType.FAILURE;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
