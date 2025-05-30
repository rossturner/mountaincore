package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureParticleEffectsComponent;

public class UpdateEveryFrameFurnitureBehaviour extends FurnitureBehaviour {

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public void update(float deltaTime) {
		FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
		if (particleEffectsComponent != null) {
			particleEffectsComponent.triggerPermanentEffects();
		}
	}

}
