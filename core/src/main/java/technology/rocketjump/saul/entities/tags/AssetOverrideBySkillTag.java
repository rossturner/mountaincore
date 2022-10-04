package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;

public class AssetOverrideBySkillTag extends Tag {


	@Override
	public String getTagName() {
		return "ASSET_OVERRIDE_BY_SKILL";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return getSkill(tagProcessingUtils.skillDictionary) != null && getColoringLayer() != null;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
	}

	public Skill getSkill(SkillDictionary skillDictionary) {
		return skillDictionary.getByName(args.get(0));
	}

	public ColoringLayer getColoringLayer() {
		return EnumUtils.getEnum(ColoringLayer.class, args.get(1));
	}
}
