package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

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
								float o1Distance = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().dst2(
										o1.getLocationComponent().getWorldOrParentPosition());
								float o2Distance = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().dst2(
										o2.getLocationComponent().getWorldOrParentPosition()
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
