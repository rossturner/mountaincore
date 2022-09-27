package technology.rocketjump.saul.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import technology.rocketjump.saul.entities.ai.goap.condition.GoalSelectionByInventory;
import technology.rocketjump.saul.entities.ai.goap.condition.GoalSelectionByWeaponAmmo;
import technology.rocketjump.saul.entities.ai.goap.condition.HasLiquidContainerNeedingFilling;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = GoalSelectionByMemory.class, name = "MEMORY"),
		@JsonSubTypes.Type(value = GoalSelectionByNeed.class, name = "NEED"),
		@JsonSubTypes.Type(value = GoalSelectionByItemAssignment.class, name = "ITEM_ASSIGNED"),
		@JsonSubTypes.Type(value = HasLiquidContainerNeedingFilling.class, name = "HAS_LIQUID_CONTAINER_NEEDING_FILLING"),
		@JsonSubTypes.Type(value = GoalSelectionByInventory.class, name = "INVENTORY"),
		@JsonSubTypes.Type(value = GoalSelectionBySquadOrders.class, name = "SQUAD_HAS_ORDERS"),
		@JsonSubTypes.Type(value = GoalSelectionByCombatSkillLevel.class, name = "COMBAT_SKILL_LEVEL"),
		@JsonSubTypes.Type(value = GoalSelectionByWeaponAmmo.class, name = "ASSIGNED_WEAPON_REQUIRES_AMMO"),
		@JsonSubTypes.Type(value = GoalSelectionByBodyPartFunction.class, name = "BODY_PART_FUNCTION"),
})
public interface GoalSelectionCondition {

	@JsonIgnore
	boolean apply(Entity parentEntity, GameContext gameContext);

}
