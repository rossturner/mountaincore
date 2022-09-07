package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Skill;

public class AssetOverrideBySkillTag extends Tag {

	private Skill skill;
	private ColoringLayer coloringLayer;

	@Override
	public String getTagName() {
		return "ASSET_OVERRIDE_BY_SKILL";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() == 2;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.skill = tagProcessingUtils.skillDictionary.getByName(args.get(0));
		if (this.skill == null) {
			Logger.error("Could not find skill with name " + args.get(0) + " for " + getTagName());
		}
		this.coloringLayer = EnumUtils.getEnum(ColoringLayer.class, args.get(1));
		if (this.coloringLayer == null) {
			Logger.error("Could not find coloring layer with name " + args.get(1) + " for " + getTagName());
		}
	}

	public Skill getSkill() {
		return skill;
	}

	public ColoringLayer getColoringLayer() {
		return coloringLayer;
	}
}
