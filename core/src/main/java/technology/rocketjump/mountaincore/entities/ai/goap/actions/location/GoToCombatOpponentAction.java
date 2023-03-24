package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.misc.VectorUtils;

import java.util.Comparator;
import java.util.List;

import static technology.rocketjump.mountaincore.entities.planning.PathfindingFlag.AVOID_COMBATANTS_HOLDING_TILES;

public class GoToCombatOpponentAction extends GoToLocationAction {

	public GoToCombatOpponentAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		this.pathfindingFlags = List.of(AVOID_COMBATANTS_HOLDING_TILES);

		CombatStateComponent combatStateComponent = parent.parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent == null || combatStateComponent.getTargetedOpponentId() == null) {
			return null;
		}
		Entity opponentEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
		if (opponentEntity == null) {
			return null;
		}
		Vector2 parentPosition = parent.parentEntity.getLocationComponent().getWorldOrParentPosition();
		GridPoint2 parentTile = VectorUtils.toGridPoint(parentPosition);
		Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
		GridPoint2 opponentTile = VectorUtils.toGridPoint(opponentPosition);

		return gameContext.getAreaMap().getNeighbours(opponentTile)
				.values().stream()
				.filter(neighbour -> neighbour.isNavigable(parent.parentEntity))
				.filter(neighbour -> neighbour.getEntities()
						.stream().noneMatch(entity -> {
							CombatStateComponent neighbourTileEntityCombatState = entity.getComponent(CombatStateComponent.class);
							return neighbourTileEntityCombatState != null && neighbour.getTilePosition().equals(neighbourTileEntityCombatState.getHeldLocation());
						})
				)
				.sorted(Comparator.comparingInt(o -> (int) (o.getTilePosition().dst2(parentTile) * 1000)))
				.map(tile -> VectorUtils.toVector(tile.getTilePosition()))
				.findFirst()
				.orElse(null);
	}

}
