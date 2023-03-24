package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

import static technology.rocketjump.mountaincore.entities.model.EntityType.FURNITURE;

public class PermanentParticleEffectsTag extends Tag {

	@Override
	public String getTagName() {
		return "PERMANENT_PARTICLE_EFFECTS";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		for (String arg : getArgs()) {
			if (tagProcessingUtils.particleEffectTypeDictionary.getByName(arg) == null) {
				Logger.error("Can not find particle effect with name " + arg + " for " + this.getClass().getSimpleName());
				return false;
			}
		}
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getType().equals(FURNITURE)) {
			FurnitureParticleEffectsComponent particleEffectsComponent = entity.getOrCreateComponent(FurnitureParticleEffectsComponent.class);
			particleEffectsComponent.init(entity, messageDispatcher, gameContext);
			for (String arg : getArgs()) {
				particleEffectsComponent.getPermanentParticleEffects().add(tagProcessingUtils.particleEffectTypeDictionary.getByName(arg));
			}
		} else {
			Logger.error(this.getClass().getSimpleName() + " must apply to a furniture entity");
		}
	}

}
