package technology.rocketjump.mountaincore.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.mountaincore.entities.ai.goap.Operator;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoalSelectionBySkill implements GoalSelectionCondition {

    public final String skillName;
    public final Operator operator;
    public final Integer value;
    public final Integer targetQuantity;

    @JsonCreator
    public GoalSelectionBySkill(
            @JsonProperty("skillName") String skillName,
            @JsonProperty("operator") Operator operator,
            @JsonProperty("value") Integer value,
            @JsonProperty("targetQuantity") Integer targetQuantity) {
        this.skillName = skillName;
        this.operator = operator;
        this.value = value;
        this.targetQuantity = targetQuantity;
    }

    @JsonIgnore
    @Override
    public boolean apply(Entity parentEntity, GameContext gameContext) {
        SkillsComponent skillsComponent = parentEntity.getComponent(SkillsComponent.class);
        if (skillsComponent != null) {
            Integer skillLevel = skillsComponent.getAll().stream()
                    .filter(e -> e.getKey().getName().equals(skillName))
                    .map(e -> e.getValue())
                    .findFirst().orElse(0);
            return operator.apply(skillLevel, value);
        }
        return false;
    }

}
