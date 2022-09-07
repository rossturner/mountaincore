package technology.rocketjump.saul.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.military.model.Squad;

import java.util.Set;

import static technology.rocketjump.saul.rendering.utils.HexColors.NEGATIVE_COLOR;

public class SelectableOutlineRenderer {

	public void render(Selectable selectable, ShapeRenderer shapeRenderer, GameContext gameContext) {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);

		switch (selectable.type) {
			case SQUAD:
				Squad squad = selectable.getSquad();
				for (Long memberEntityId : squad.getMemberEntityIds()) {
					Entity squadMember = gameContext.getEntities().get(memberEntityId);
					if (squadMember != null) {
						renderCircleAroundEntity(squadMember, shapeRenderer, gameContext);
					}
				}
				break;
			case ENTITY:
				break;
			case ROOM:
				renderTileOutline(selectable.getRoom().getRoomTiles().keySet(), shapeRenderer);
				break;
			case BRIDGE:
				renderTileOutline(selectable.getBridge().getLocations(), shapeRenderer);
				break;
			case TILE:
				renderTileOutline(selectable.getTile().getTilePosition(), shapeRenderer);
				break;
			case DOORWAY:
				renderTileOutline(selectable.getDoorway().getTileLocation(), shapeRenderer);
				break;
			case CONSTRUCTION:
				renderTileOutline(selectable.getConstruction().getTileLocations(), shapeRenderer);
				break;
			default:
				Logger.error("Not yet implemented: " + this.getClass().getSimpleName() + ".render() for " + selectable.type);
		}

		shapeRenderer.end();
	}

	private void renderCircleAroundEntity(Entity selectedEntity, ShapeRenderer shapeRenderer, GameContext gameContext) {
		Vector2 worldPosition = selectedEntity.getLocationComponent().getWorldPosition();
		if (worldPosition == null) {
			return;
		}
		shapeRenderer.circle(worldPosition.x, worldPosition.y,
				selectedEntity.getLocationComponent().getRadius() + 0.3f, 100);
		CombatStateComponent combatStateComponent = selectedEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent != null && combatStateComponent.isInCombat() && combatStateComponent.getTargetedOpponentId() != null) {
			Entity targetEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
			if (targetEntity != null) {
				Vector2 targetPosition = targetEntity.getLocationComponent().getWorldPosition();
				if (targetPosition != null) {
					shapeRenderer.setColor(NEGATIVE_COLOR);
					shapeRenderer.circle(targetPosition.x, targetPosition.y,
							targetEntity.getLocationComponent().getRadius() + 0.3f, 100);
					shapeRenderer.setColor(Color.WHITE);
				}
			}
		}
	}

	private void renderTileOutline(GridPoint2 tileLocation, ShapeRenderer shapeRenderer) {
		shapeRenderer.rect(tileLocation.x, tileLocation.y, 1,1);
	}

	private void renderTileOutline(Set<GridPoint2> tileLocations, ShapeRenderer shapeRenderer) {
		for (GridPoint2 tileLocation : tileLocations) {

			// Draw top
			if (!tileLocations.contains(tileLocation.cpy().add(0, 1))) {
				shapeRenderer.line(tileLocation.x, tileLocation.y + 1, tileLocation.x + 1, tileLocation.y + 1);
			}
			// draw right
			if (!tileLocations.contains(tileLocation.cpy().add(1, 0))) {
				shapeRenderer.line(tileLocation.x + 1, tileLocation.y, tileLocation.x + 1, tileLocation.y + 1);
			}
			// draw left
			if (!tileLocations.contains(tileLocation.cpy().add(-1, 0))) {
				shapeRenderer.line(tileLocation.x, tileLocation.y, tileLocation.x, tileLocation.y + 1);
			}
			// draw bottom
			if (!tileLocations.contains(tileLocation.cpy().add(0, -1))) {
				shapeRenderer.line(tileLocation.x, tileLocation.y, tileLocation.x + 1, tileLocation.y);
			}

		}
	}

}
