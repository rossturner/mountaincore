package technology.rocketjump.mountaincore.entities.behaviour.items;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.combat.model.WeaponAttack;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.CombatAttackMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.HashSet;
import java.util.Set;

public class ProjectileBehaviour implements BehaviourComponent {

	private static final float PROJECTILE_SPEED = 10f;

	private LocationComponent locationComponent;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;
	private Entity parentEntity;
	private Entity attackerEntity;
	private Entity defenderEntity;
	private WeaponAttack weaponAttack;
	private Set<Long> otherEntitiesEncountered = new HashSet<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.locationComponent = parentEntity.getLocationComponent();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
		otherEntitiesEncountered.add(parentEntity.getId());
	}

	@Override
	public ProjectileBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ProjectileBehaviour cloned = new ProjectileBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		cloned.attackerEntity = this.attackerEntity;
		cloned.defenderEntity = this.defenderEntity;
		cloned.weaponAttack = this.weaponAttack;
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		// move towards target and set rotation to target
		Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
		Vector2 targetPosition = defenderEntity.getLocationComponent().getWorldOrParentPosition();

		float separation = parentPosition.dst(targetPosition);
		Vector2 targetVector = targetPosition.cpy().sub(parentPosition);
		targetVector.nor().scl(deltaTime * PROJECTILE_SPEED);

		if (targetVector.len() >= separation) {
			// going to hit this frame
			impactWith(defenderEntity);
		} else {
			Vector2 newPosition = parentPosition.cpy().add(targetVector);
			parentEntity.getLocationComponent().setWorldPosition(newPosition, false);
			parentEntity.getLocationComponent().setRotation(targetVector.angleDeg());

			MapTile tile = gameContext.getAreaMap().getTile(newPosition);
			if (tile != null) {
				for (Entity otherEntity : tile.getEntities()) {
					if (otherEntity.getId() == parentEntity.getId()) {
						continue;
					}
					if (otherEntitiesEncountered.contains(otherEntity.getId())) {
						continue;
					}

					if (otherEntity.getLocationComponent().getWorldOrParentPosition().dst(newPosition) <
						otherEntity.getLocationComponent().getRadius()) {
						if (gameContext.getRandom().nextFloat() < chanceToHitOtherEntity(otherEntity)) {
							impactWith(otherEntity);
							break;
						} else {
							otherEntitiesEncountered.add(otherEntity.getId());
						}
					}
				}
			}
		}
	}

	@Override
	public void updateWhenPaused() {

	}

	private void impactWith(Entity impactedEntity) {
		messageDispatcher.dispatchMessage(MessageType.COMBAT_PROJECTILE_REACHED_TARGET,
				new CombatAttackMessage(attackerEntity, impactedEntity, weaponAttack,
						(ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()));
		messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
	}

	private float chanceToHitOtherEntity(Entity otherEntity) {
		return 0.08f;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	public void setAttackerEntity(Entity attackerEntity) {
		this.attackerEntity = attackerEntity;
		this.otherEntitiesEncountered.add(attackerEntity.getId());
	}

	public Entity getAttackerEntity() {
		return attackerEntity;
	}

	public void setDefenderEntity(Entity defenderEntity) {
		this.defenderEntity = defenderEntity;
	}

	public Entity getDefenderEntity() {
		return defenderEntity;
	}

	public void setWeaponAttack(WeaponAttack weaponAttack) {
		this.weaponAttack = weaponAttack;
	}

	public WeaponAttack getWeaponAttack() {
		return weaponAttack;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		attackerEntity.writeTo(savedGameStateHolder);
		asJson.put("attacker", attackerEntity.getId());

		defenderEntity.writeTo(savedGameStateHolder);
		asJson.put("defender", defenderEntity.getId());

		JSONObject weaponAttackJson = new JSONObject(true);
		weaponAttack.writeTo(weaponAttackJson, savedGameStateHolder);
		asJson.put("weaponAttack", weaponAttackJson);

		JSONArray otherEntitiesJson = new JSONArray();
		for (Long entityId : otherEntitiesEncountered) {
			otherEntitiesJson.add(entityId);
		}
		if (!otherEntitiesJson.isEmpty()) {
			asJson.put("encountered", otherEntitiesJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.attackerEntity = savedGameStateHolder.entities.get(asJson.getLong("attacker"));
		if (this.attackerEntity == null) {
			throw new InvalidSaveException("Could not find entity with ID " + asJson.getLong("attacker"));
		}

		this.defenderEntity = savedGameStateHolder.entities.get(asJson.getLong("defender"));
		if (this.defenderEntity == null) {
			throw new InvalidSaveException("Could not find entity with ID " + asJson.getLong("defender"));
		}

		JSONObject weaponAttackJson = asJson.getJSONObject("weaponAttack");
		this.weaponAttack = new WeaponAttack();
		this.weaponAttack.readFrom(weaponAttackJson, savedGameStateHolder, relatedStores);

		JSONArray otherEntitiesJson = asJson.getJSONArray("encountered");
		if (otherEntitiesJson != null) {
			for (int cursor = 0; cursor < otherEntitiesJson.size(); cursor++) {
				this.otherEntitiesEncountered.add(otherEntitiesJson.getLong(cursor));
			}
		}
	}

}
