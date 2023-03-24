package technology.rocketjump.mountaincore.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.mountaincore.entities.ai.goap.Operator;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoalSelectionByCombatSkillLevel implements GoalSelectionCondition {

	public final Operator operator;
	public final Double value;

	@JsonCreator
	public GoalSelectionByCombatSkillLevel(
			@JsonProperty("operator") Operator operator,
			@JsonProperty("value") Double value) {
		this.operator = operator;
		this.value = value;
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent == null) {
			return false;
		} else {
			Long assignedWeaponId = militaryComponent.getAssignedWeaponId();
			SkillsComponent skillsComponent = parentEntity.getComponent(SkillsComponent.class);
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			int skillLevel = skillsComponent.getSkillLevel(attributes.getRace().getFeatures().getUnarmedWeapon().getCombatSkill());
			if (assignedWeaponId != null) {
				Entity weaponEntity = gameContext.getEntities().get(assignedWeaponId);
				if (weaponEntity != null && weaponEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes weaponAttributes &&
						weaponAttributes.getItemType().getWeaponInfo() != null) {
					skillLevel = skillsComponent.getSkillLevel(weaponAttributes.getItemType().getWeaponInfo().getCombatSkill());
				}
			}
			return operator.apply(skillLevel, value);
		}
	}

}
